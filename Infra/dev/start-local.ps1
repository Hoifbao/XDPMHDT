# =====================================================================
# Smart Heritage - Chay local day du (Bao - Sprint 4). PowerShell 5.1.
#
#   Cong chinh cho Web Admin / Mobile / Postman: http://localhost:8080 (api-gateway)
#     8080 api-gateway  -> /api/auth/** sang auth-service 8081, con lai sang Backend cu 8090
#     8081 auth-service
#     8090 Backend cu (monolith) - chi goi thang khi debug / xem Swagger
#
# Chay o thu muc goc repo:
#   powershell -ExecutionPolicy Bypass -File Infra\dev\start-local.ps1
# Tuy chon:
#   -SkipInstall   bo qua "mvn install" (da cai module common roi)
# Moi thanh phan mo 1 cua so rieng; tat bang Ctrl+C trong tung cua so.
# =====================================================================
param([switch]$SkipInstall)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $root

function Get-PortPid([int]$port) {
    $line = netstat -ano | Select-String -Pattern "^\s*TCP\s+\S+:$port\s+\S+\s+LISTENING\s+(\d+)" | Select-Object -First 1
    if ($line) { return [int]$line.Matches[0].Groups[1].Value }
    return $null
}

# 1. Cong 8080/8081/8090 phai trong
$busy = @()
foreach ($p in 8080, 8081, 8090) {
    $procId = Get-PortPid $p
    if ($procId) {
        $name = (Get-Process -Id $procId -ErrorAction SilentlyContinue).ProcessName
        $busy += "  cong $p dang bi PID $procId ($name) chiem"
    }
}
if ($busy.Count) {
    Write-Host "Cong dang ban, tat truoc roi chay lai:" -ForegroundColor Red
    $busy | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    Write-Host "  Vi du: Stop-Process -Id <PID> -Force   (hoac Ctrl+C o cua so dang chay)" -ForegroundColor Yellow
    Write-Host "  Neu la Docker profile core: docker compose -f Infra/docker-compose.yml --profile core stop backend auth-service api-gateway" -ForegroundColor Yellow
    exit 1
}

# 2. MySQL + Redis
Write-Host "[1/4] Bat MySQL (3307) + Redis (6379)..." -ForegroundColor Cyan
docker compose -f Infra/docker-compose.yml up -d mysql-db redis-cache
if ($LASTEXITCODE -ne 0) { Write-Host "Docker loi: mo Docker Desktop roi chay lai." -ForegroundColor Red; exit 1 }

# 3. Cai module common (can cho auth-service, api-gateway)
if (-not $SkipInstall) {
    Write-Host "[2/4] mvn install (module common)..." -ForegroundColor Cyan
    mvn -q -DskipTests install
    if ($LASTEXITCODE -ne 0) { Write-Host "mvn install loi, xem log phia tren." -ForegroundColor Red; exit 1 }
}

# 4. Mo 3 cua so: Backend cu, auth-service, gateway
Write-Host "[3/4] Mo 3 cua so: Backend 8090, auth-service 8081, api-gateway 8080..." -ForegroundColor Cyan
function Start-Win([string]$title, [string]$dir, [string]$cmd) {
    $script = "`$host.UI.RawUI.WindowTitle = '$title'; Set-Location '$dir'; $cmd"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $script | Out-Null
}
Start-Win "Backend cu 8090" (Join-Path $root "Backend") "mvn spring-boot:run"
Start-Win "auth-service 8081" $root "mvn -pl services/auth-service spring-boot:run"
Start-Win "api-gateway 8080" $root "mvn -pl services/api-gateway spring-boot:run"

# 5. Doi ca 3 len roi kiem nhanh qua cong 8080
Write-Host "[4/4] Doi ca 3 khoi dong (toi da 3 phut)..." -ForegroundColor Cyan
$deadline = (Get-Date).AddMinutes(3)
while ((Get-Date) -lt $deadline) {
    if ((Get-PortPid 8080) -and (Get-PortPid 8081) -and (Get-PortPid 8090)) { break }
    Start-Sleep -Seconds 3
}
Start-Sleep -Seconds 3

function Try-Status([scriptblock]$call) {
    try { $r = & $call; return [int]$r.StatusCode } catch {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode } return 0 }
}
$s1 = Try-Status { Invoke-WebRequest "http://localhost:8080/api/heritage-sites" -UseBasicParsing }
$login = $null
$s2 = Try-Status {
    $r = Invoke-WebRequest "http://localhost:8080/api/auth/login" -Method POST -UseBasicParsing `
        -ContentType "application/json" -Body '{"username":"admin","password":"Test@1234"}'
    $script:login = ($r.Content | ConvertFrom-Json).accessToken; $r }
$s3 = Try-Status { Invoke-WebRequest "http://localhost:8080/api/statistics/overview" -UseBasicParsing -Headers @{ Authorization = "Bearer $login" } }

Write-Host ""
Write-Host ("  8080 -> Backend cu   GET  /api/heritage-sites      : {0}" -f $s1)
Write-Host ("  8080 -> auth-service POST /api/auth/login (admin)  : {0}" -f $s2)
Write-Host ("  8080 -> Backend cu   GET  /api/statistics/overview : {0}  (co token admin)" -f $s3)
if ($s1 -eq 200 -and $s2 -eq 200 -and $s3 -eq 200) {
    Write-Host "San sang. Web Admin / Mobile / Postman goi http://localhost:8080" -ForegroundColor Green
    Write-Host "Swagger (Backend cu): http://localhost:8090/swagger-ui/index.html" -ForegroundColor Green
} else {
    Write-Host "Chua san sang: xem log o 3 cua so vua mo (thuong la dang khoi dong cham, doi them roi chay lai buoc kiem)." -ForegroundColor Yellow
}
