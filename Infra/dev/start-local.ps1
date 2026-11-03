# =====================================================================
# Smart Heritage - Chay local day du (Bao - Sprint 4, bo sung Phu W5). PowerShell 5.1.
#
#   Cong chinh cho Web Admin / Mobile / Postman: http://localhost:8080 (api-gateway)
#     8080 api-gateway  -> /api/auth/** sang auth-service 8081, /api/heritage-sites/** sang site-service 8082,
#                           /api/me/**, /api/admin/visits/** sang personalization-service 8085,
#                           /api/artifacts/*/feedbacks, /api/artifacts/*/rating-summary, /api/admin/feedbacks/**
#                           sang feedback-service 8086, con lai sang Backend cu 8090
#     8081 auth-service
#     8082 site-service (di tich + /nearby)
#     8085 personalization-service (favorites, visit history, offline sync)
#     8086 feedback-service (danh gia, diem trung binh, Admin duyet/an)
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

# 1. Cac cong service local phai trong
$busy = @()
foreach ($p in 8080, 8081, 8082, 8085, 8086, 8090) {
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
    exit 1
}

# 2. MySQL + Redis
Write-Host "[1/4] Bat MySQL (3307) + Redis (6379)..." -ForegroundColor Cyan
docker compose -f Infra/docker-compose.yml up -d mysql-db redis-cache
if ($LASTEXITCODE -ne 0) { Write-Host "Docker loi: mo Docker Desktop roi chay lai." -ForegroundColor Red; exit 1 }

# 3. Cai cac module (common + service moi)
if (-not $SkipInstall) {
    Write-Host "[2/4] mvn install..." -ForegroundColor Cyan
    mvn -q -DskipTests install
    if ($LASTEXITCODE -ne 0) { Write-Host "mvn install loi, xem log phia tren." -ForegroundColor Red; exit 1 }
}

# 4. Mo 6 cua so
Write-Host "[3/4] Mo 6 cua so: Backend 8090, auth 8081, gateway 8080, site 8082, personalization 8085, feedback 8086..." -ForegroundColor Cyan
function Start-Win([string]$title, [string]$dir, [string]$cmd) {
    $script = "`$host.UI.RawUI.WindowTitle = '$title'; Set-Location '$dir'; $cmd"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $script | Out-Null
}
Start-Win "Backend cu 8090" (Join-Path $root "Backend") "mvn spring-boot:run"
Start-Win "auth-service 8081" $root "mvn -pl services/auth-service spring-boot:run"
Start-Win "api-gateway 8080" $root "`$env:FEEDBACK_SERVICE_URL='http://localhost:8086'; `$env:PERSONALIZATION_SERVICE_URL='http://localhost:8085'; mvn -pl services/api-gateway spring-boot:run"
Start-Win "site-service 8082" $root "mvn -pl services/site-service spring-boot:run"
# start-local chua bat content-service 8083, nen personalization/feedback tam hoi hien vat qua Backend cu 8090.
Start-Win "personalization-service 8085" $root "`$env:CONTENT_SERVICE_URL='http://localhost:8090'; mvn -pl services/personalization-service spring-boot:run"
Start-Win "feedback-service 8086" $root "`$env:CONTENT_SERVICE_URL='http://localhost:8090'; mvn -pl services/feedback-service spring-boot:run"

# 5. Doi ca 6 len roi kiem nhanh qua cong 8080
Write-Host "[4/4] Doi ca 6 khoi dong (toi da 3 phut)..." -ForegroundColor Cyan
$deadline = (Get-Date).AddMinutes(3)
while ((Get-Date) -lt $deadline) {
    if ((Get-PortPid 8080) -and (Get-PortPid 8081) -and (Get-PortPid 8082) -and (Get-PortPid 8085) -and (Get-PortPid 8086) -and (Get-PortPid 8090)) { break }
    Start-Sleep -Seconds 3
}
Start-Sleep -Seconds 3

function Try-Status([scriptblock]$call) {
    try { $r = & $call; return [int]$r.StatusCode } catch {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode } return 0
    }
}
$s1 = Try-Status { Invoke-WebRequest "http://localhost:8080/api/heritage-sites" -UseBasicParsing }
$login = $null
$s2 = Try-Status {
    $r = Invoke-WebRequest "http://localhost:8080/api/auth/login" -Method POST -UseBasicParsing `
        -ContentType "application/json" -Body '{"username":"admin","password":"Test@1234"}'
    $json = $r.Content | ConvertFrom-Json
    $script:login = if ($json.accessToken) { $json.accessToken } else { $json.data.accessToken }
    $r }
$s3 = Try-Status { Invoke-WebRequest "http://localhost:8080/api/statistics/overview" -UseBasicParsing -Headers @{ Authorization = "Bearer $login" } }
$s4 = Try-Status { Invoke-WebRequest "http://localhost:8080/api/artifacts/1/feedbacks" -UseBasicParsing }
$s5 = Try-Status { Invoke-WebRequest "http://localhost:8080/api/artifacts/1/rating-summary" -UseBasicParsing }
$s6 = Try-Status { Invoke-WebRequest "http://localhost:8080/api/me/favorites?page=0&size=1" -UseBasicParsing -Headers @{ Authorization = "Bearer $login" } }

Write-Host ""
Write-Host ("  8080 -> site-service            GET  /api/heritage-sites              : {0}" -f $s1)
Write-Host ("  8080 -> auth-service            POST /api/auth/login (admin)          : {0}" -f $s2)
Write-Host ("  8080 -> Backend cu              GET  /api/statistics/overview         : {0}" -f $s3)
Write-Host ("  8080 -> feedback-service        GET  /api/artifacts/1/feedbacks       : {0}" -f $s4)
Write-Host ("  8080 -> feedback-service        GET  /api/artifacts/1/rating-summary  : {0}" -f $s5)
Write-Host ("  8080 -> personalization-service GET  /api/me/favorites                 : {0}" -f $s6)
if ($s1 -eq 200 -and $s2 -eq 200 -and $s3 -eq 200 -and $s4 -eq 200 -and $s5 -eq 200 -and $s6 -eq 200) {
    Write-Host "San sang. Web Admin / Mobile / Postman goi http://localhost:8080" -ForegroundColor Green
    Write-Host "Swagger chung (chon service o goc tren ben phai): http://localhost:8080/swagger-ui.html" -ForegroundColor Green
    Write-Host "Swagger (Backend cu): http://localhost:8090/swagger-ui/index.html" -ForegroundColor Green
    Write-Host "Swagger (site-service): http://localhost:8082/swagger-ui.html" -ForegroundColor Green
    Write-Host "Swagger (personalization-service): http://localhost:8085/swagger-ui.html" -ForegroundColor Green
    Write-Host "Swagger (feedback-service): http://localhost:8086/swagger-ui.html" -ForegroundColor Green
} else {
    Write-Host "Chua san sang: xem log o cac cua so vua mo (thuong la dang khoi dong cham, doi them roi chay lai buoc kiem)." -ForegroundColor Yellow
}
