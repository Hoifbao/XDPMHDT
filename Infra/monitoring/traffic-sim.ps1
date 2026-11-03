# =====================================================================
# Gia lap traffic cho demo Monitoring (Bao - Sprint 5). PowerShell 5.1+.
# Goi API qua api-gateway de so lieu tren Prometheus/Grafana thay doi.
#
# Chay o thu muc goc repo:
#   powershell -ExecutionPolicy Bypass -File Infra/monitoring/traffic-sim.ps1                  # normal, chay mai, Ctrl+C de dung
#   powershell -ExecutionPolicy Bypass -File Infra/monitoring/traffic-sim.ps1 -Mode spike      # tang tai dot ngot 60 giay
#   powershell -ExecutionPolicy Bypass -File Infra/monitoring/traffic-sim.ps1 -Mode error      # tron loi 5xx
#   powershell -ExecutionPolicy Bypass -File Infra/monitoring/traffic-sim.ps1 -Mode scenario   # kich ban 5 phut de quay video
#
# Tuy chon: -Seconds <so giay> (0 = chay mai)  -BaseUrl http://localhost:8080  -Workers 8 (spike)
#
# Che do:
#   normal   : ~3 request/giay. 70% GET /api/artifacts, 15% GET /api/artifacts/{1..3},
#              10% GET /api/auth/me khong token (-> 401), 5% POST /api/auth/login admin (-> 200, co truy van DB).
#   spike    : nhieu luong goi /api/artifacts lien tuc -> request/giay va p95 tang, HikariCP ban hon.
#   error    : nhu normal + 1/2 request vao /api/khong-ton-tai. Gateway chuyen ve Backend cu 8090;
#              Backend cu TAT -> 500 (canh bao HighErrorRate5xx). Backend cu BAT -> 404 (chi tang 4xx).
#   scenario : normal 2' -> spike 1' -> normal 1' -> error 1' -> normal (Ctrl+C de dung).
# Chi doc/goi API, khong sua du lieu (login khong tao gi moi).
# =====================================================================
param(
    [ValidateSet("normal", "spike", "error", "scenario")]
    [string]$Mode = "normal",
    [int]$Seconds = 0,
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Workers = 8,
    [string]$Username = "admin",
    [string]$Password = "Test@1234"
)

Add-Type -AssemblyName System.Net.Http
$BaseUrl = $BaseUrl.TrimEnd("/")
$script:stats = @{}
$script:total = 0
$script:started = Get-Date

function New-Client {
    $c = New-Object System.Net.Http.HttpClient
    $c.Timeout = [TimeSpan]::FromSeconds(10)
    return $c
}
$script:client = New-Client
$loginBody = '{"username":"' + $Username + '","password":"' + $Password + '"}'

function Send-Request([string]$method, [string]$path) {
    try {
        if ($method -eq "POST") {
            $content = New-Object System.Net.Http.StringContent($loginBody, [Text.Encoding]::UTF8, "application/json")
            $resp = $script:client.PostAsync($BaseUrl + $path, $content).GetAwaiter().GetResult()
        } else {
            $resp = $script:client.GetAsync($BaseUrl + $path).GetAwaiter().GetResult()
        }
        $code = [int]$resp.StatusCode
        $resp.Dispose()
        return $code
    } catch { return 0 }   # 0 = khong ket noi duoc / timeout
}

function Add-Stat([int]$code) {
    $key = if ($code -eq 0) { "loi_ket_noi" } else { [string]$code }
    if ($script:stats.ContainsKey($key)) { $script:stats[$key]++ } else { $script:stats[$key] = 1 }
    $script:total++
}

function Show-Stats([string]$phase) {
    $parts = $script:stats.Keys | Sort-Object | ForEach-Object { "$_=$($script:stats[$_])" }
    Write-Host ("{0} [{1}] tong {2} request | {3}" -f (Get-Date -Format "HH:mm:ss"), $phase, $script:total, ($parts -join "  "))
}

function Test-Port([int]$port) {
    $t = New-Object System.Net.Sockets.TcpClient
    try { $t.Connect("localhost", $port); return $true } catch { return $false } finally { $t.Close() }
}

# Chon 1 request theo ty le cua che do normal / error
function Get-NextRequest([bool]$withErrors) {
    $r = Get-Random -Minimum 0 -Maximum 100
    if ($withErrors -and ($r % 2 -eq 0)) { return @("GET", "/api/khong-ton-tai") }
    if ($r -lt 70) { return @("GET", "/api/artifacts") }
    if ($r -lt 85) { return @("GET", "/api/artifacts/" + (Get-Random -Minimum 1 -Maximum 4)) }
    if ($r -lt 95) { return @("GET", "/api/auth/me") }
    return @("POST", "/api/auth/login")
}

function Invoke-Steady([string]$phase, [bool]$withErrors, [int]$durationSec) {
    $deadline = if ($durationSec -gt 0) { (Get-Date).AddSeconds($durationSec) } else { [DateTime]::MaxValue }
    $nextPrint = (Get-Date).AddSeconds(5)
    while ((Get-Date) -lt $deadline) {
        $req = Get-NextRequest $withErrors
        Add-Stat (Send-Request $req[0] $req[1])
        if ((Get-Date) -ge $nextPrint) { Show-Stats $phase; $nextPrint = (Get-Date).AddSeconds(5) }
        Start-Sleep -Milliseconds 300
    }
}

function Invoke-Spike([int]$durationSec, [int]$workers) {
    if ($durationSec -le 0) { $durationSec = 60 }
    Write-Host "  Spike: $workers luong goi lien tuc trong $durationSec giay..." -ForegroundColor Yellow
    $worker = {
        param($url, $sec)
        Add-Type -AssemblyName System.Net.Http
        $c = New-Object System.Net.Http.HttpClient
        $c.Timeout = [TimeSpan]::FromSeconds(10)
        $end = (Get-Date).AddSeconds($sec)
        $res = @{}
        while ((Get-Date) -lt $end) {
            try { $r = $c.GetAsync($url).GetAwaiter().GetResult(); $k = [string][int]$r.StatusCode; $r.Dispose() }
            catch { $k = "loi_ket_noi" }
            if ($res.ContainsKey($k)) { $res[$k]++ } else { $res[$k] = 1 }
        }
        $c.Dispose()
        return $res
    }
    $pool = [RunspaceFactory]::CreateRunspacePool(1, $workers)
    $pool.Open()
    $jobs = @()
    for ($i = 0; $i -lt $workers; $i++) {
        $ps = [PowerShell]::Create().AddScript($worker).AddArgument("$BaseUrl/api/artifacts").AddArgument($durationSec)
        $ps.RunspacePool = $pool
        $jobs += [pscustomobject]@{ PS = $ps; Handle = $ps.BeginInvoke() }
    }
    $end = (Get-Date).AddSeconds($durationSec + 15)
    while (($jobs | Where-Object { -not $_.Handle.IsCompleted }) -and (Get-Date) -lt $end) {
        Start-Sleep -Seconds 5
        Write-Host ("{0} [spike] dang chay..." -f (Get-Date -Format "HH:mm:ss"))
    }
    foreach ($j in $jobs) {
        try {
            foreach ($h in $j.PS.EndInvoke($j.Handle)) {
                foreach ($k in $h.Keys) {
                    if ($script:stats.ContainsKey($k)) { $script:stats[$k] += $h[$k] } else { $script:stats[$k] = $h[$k] }
                    $script:total += $h[$k]
                }
            }
        } catch {}
        $j.PS.Dispose()
    }
    $pool.Close()
    Show-Stats "spike"
}

# ---------- Kiem tra truoc ----------
Write-Host "== Gia lap traffic: che do '$Mode' -> $BaseUrl" -ForegroundColor Cyan
if (-not (Test-Port ([uri]$BaseUrl).Port)) {
    Write-Host "[LOI] Khong ket noi duoc $BaseUrl - api-gateway chua chay?" -ForegroundColor Red
    exit 1
}
if ($Mode -eq "error" -or $Mode -eq "scenario") {
    if (Test-Port 8090) {
        Write-Host "[CANH BAO] Backend cu 8090 dang BAT: /api/khong-ton-tai se tra 404 (4xx), khong phai 500." -ForegroundColor Yellow
        Write-Host "           Muon thay loi 5xx va canh bao HighErrorRate5xx thi tat Backend cu truoc." -ForegroundColor Yellow
    }
}
Write-Host "Ctrl+C de dung. Moi 5 giay in thong ke ma tra ve." -ForegroundColor Gray

try {
    switch ($Mode) {
        "normal" { Invoke-Steady "normal" $false $Seconds }
        "error"  { Invoke-Steady "error" $true $Seconds }
        "spike"  { Invoke-Spike $Seconds $Workers }
        "scenario" {
            Write-Host "-- Pha 1/5: binh thuong 2 phut" -ForegroundColor Green
            Invoke-Steady "1-normal" $false 120
            Write-Host "-- Pha 2/5: TANG TAI 1 phut (xem Request/giay, p95)" -ForegroundColor Yellow
            Invoke-Spike 60 $Workers
            Write-Host "-- Pha 3/5: binh thuong 1 phut" -ForegroundColor Green
            Invoke-Steady "3-normal" $false 60
            Write-Host "-- Pha 4/5: LOI 5xx 1 phut (xem Ty le loi 5xx, tab Alerts)" -ForegroundColor Red
            Invoke-Steady "4-error" $true 60
            Write-Host "-- Pha 5/5: binh thuong (Ctrl+C de dung)" -ForegroundColor Green
            Invoke-Steady "5-normal" $false 0
        }
    }
} finally {
    $script:client.Dispose()
    $mins = [math]::Round(((Get-Date) - $script:started).TotalMinutes, 1)
    Write-Host ""
    Write-Host "Ket thuc sau $mins phut." -ForegroundColor Cyan
    Show-Stats "tong ket"
}
