# Smoke test monitoring (Bao - Sprint 5). CHI DOC, khong sua gi.
# Chay sau khi bat: docker compose -f Infra/docker-compose.yml --profile monitoring up -d
#   powershell -ExecutionPolicy Bypass -File Infra/monitoring/smoke-test.ps1
# Tuy chon: -GrafanaPassword <mat khau trong Infra/.env>  -RequireServices api-gateway,auth-service
# Ket qua: [OK] dat, [CANH BAO] nen xem, [LOI] chua dat (exit 1).

param(
    [string]$PrometheusUrl = "http://localhost:9090",
    [string]$GrafanaUrl = "http://localhost:3000",
    [string]$MysqlExporterUrl = "http://localhost:9104",
    [string]$RedisExporterUrl = "http://localhost:9121",
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$GrafanaUser = "admin",
    [string]$GrafanaPassword = "change-me-grafana",
    # Service bat buoc phai UP (mac dinh khong bat buoc - service nao chua chay chi canh bao)
    [string[]]$RequireServices = @()
)

$script:fail = 0
$script:warn = 0
function Ok($m)   { Write-Host "  [OK] $m" -ForegroundColor Green }
function Warn($m) { Write-Host "  [CANH BAO] $m" -ForegroundColor Yellow; $script:warn++ }
function Fail($m) { Write-Host "  [LOI] $m" -ForegroundColor Red; $script:fail++ }

function Get-Text($url, $headers) {
    try {
        if ($headers) { $resp = Invoke-WebRequest -Uri $url -Headers $headers -UseBasicParsing -TimeoutSec 5 }
        else { $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5 }
        # Doc theo UTF-8 (PowerShell 5.1 mac dinh doc sai tieng Viet)
        return [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
    } catch { return $null }
}
function Get-Json($url, $headers) {
    $t = Get-Text $url $headers
    if ($null -eq $t) { return $null }
    try { return ($t | ConvertFrom-Json) } catch { return $null }
}
function Prom-Query($expr) {
    $u = "$PrometheusUrl/api/v1/query?query=" + [uri]::EscapeDataString($expr)
    $r = Get-Json $u $null
    if ($null -eq $r -or $r.status -ne "success") { return $null }
    # Dau phay: giu mang rong (neu khong PowerShell bien mang rong thanh $null)
    return ,@($r.data.result)
}

# ---------- 1. Prometheus ----------
Write-Host "== 1. Prometheus ($PrometheusUrl)"
if ($null -eq (Get-Text "$PrometheusUrl/-/ready" $null)) {
    Fail "Prometheus khong phan hoi. Da bat --profile monitoring chua? (docker ps)"
} else {
    Ok "Prometheus dang chay"
    $t = Get-Json "$PrometheusUrl/api/v1/targets?state=active" $null
    if ($null -eq $t) { Fail "Khong doc duoc danh sach target" }
    else {
        foreach ($tg in ($t.data.activeTargets | Sort-Object { $_.labels.service })) {
            $svc = $tg.labels.service
            $line = "{0,-24} {1,-5} {2}" -f $svc, $tg.health, $tg.scrapeUrl
            if ($tg.health -eq "up") { Ok $line }
            elseif (($svc -eq "mysql") -or ($svc -eq "redis") -or ($RequireServices -contains $svc)) {
                Fail "$line  loi: $($tg.lastError)"
            }
            else { Warn "$line  (service chua chay?)" }
        }
    }
}

# ---------- 2. Exporter ----------
Write-Host "== 2. Exporter MySQL / Redis"
$m = Get-Text "$MysqlExporterUrl/metrics" $null
if ($null -eq $m) { Fail "mysql-exporter khong phan hoi ($MysqlExporterUrl)" }
elseif ($m -match "(?m)^mysql_up 1") { Ok "mysql_up = 1 (exporter ket noi duoc MySQL)" }
else { Fail "mysql_up khac 1: sai mat khau MYSQL_ROOT_PASSWORD hoac MySQL chua san sang (docker logs smart_heritage_mysql_exporter)" }

$r = Get-Text "$RedisExporterUrl/metrics" $null
if ($null -eq $r) { Fail "redis-exporter khong phan hoi ($RedisExporterUrl)" }
elseif ($r -match "(?m)^redis_up 1") { Ok "redis_up = 1 (exporter ket noi duoc Redis)" }
else { Fail "redis_up khac 1 (docker logs smart_heritage_redis_exporter)" }

# ---------- 3. Histogram p95 ----------
Write-Host "== 3. Histogram p95 (http_server_requests_seconds_bucket)"
$up = Prom-Query 'up{job="smart-heritage"} == 1'
if ($null -eq $up) { Warn "Khong truy van duoc Prometheus" }
elseif ($up.Count -eq 0) { Warn "Chua co service nao UP - bo qua kiem tra p95" }
else {
    $hist = Prom-Query 'count by (service) (http_server_requests_seconds_bucket{job="smart-heritage"})'
    $withHist = @($hist | ForEach-Object { $_.metric.service })
    foreach ($s in $up) {
        $name = $s.metric.service
        if ($withHist -contains $name) { Ok "$name co histogram -> ve duoc p95" }
        else { Warn "$name chua co histogram: chua bat percentiles-histogram hoac chua co request nao (goi thu 1 API roi chay lai)" }
    }
}

# ---------- 3b. Quy tac canh bao ----------
Write-Host "== 3b. Quy tac canh bao (alerts.yml)"
$rules = Get-Json "$PrometheusUrl/api/v1/rules?type=alert" $null
if ($null -eq $rules -or $rules.status -ne "success") { Warn "Khong doc duoc /api/v1/rules" }
else {
    $names = @($rules.data.groups | ForEach-Object { $_.rules } | ForEach-Object { $_.name })
    if ($names.Count -eq 0) { Fail "Prometheus chua nap quy tac canh bao nao (kiem tra rule_files trong prometheus.yml va mount alerts.yml)" }
    else {
        Ok ("Da nap {0} quy tac: {1}" -f $names.Count, ($names -join ", "))
        $firing = @($rules.data.groups | ForEach-Object { $_.rules } | Where-Object { $_.state -eq "firing" } | ForEach-Object { $_.name })
        if ($firing.Count -gt 0) { Warn ("Canh bao dang FIRING: " + ($firing -join ", ") + " -> xem $PrometheusUrl/alerts") }
    }
}

# ---------- 4. Grafana ----------
Write-Host "== 4. Grafana ($GrafanaUrl)"
$h = Get-Json "$GrafanaUrl/api/health" $null
if ($null -eq $h) { Fail "Grafana khong phan hoi" }
else {
    Ok "Grafana dang chay (database: $($h.database), version $($h.version))"
    $pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${GrafanaUser}:${GrafanaPassword}"))
    $auth = @{ Authorization = "Basic $pair" }
    $ds = Get-Json "$GrafanaUrl/api/datasources/name/Prometheus" $auth
    if ($null -eq $ds) { Fail "Khong thay datasource Prometheus (hoac sai mat khau Grafana: dung -GrafanaPassword)" }
    else { Ok "Datasource Prometheus -> $($ds.url)" }
    foreach ($uid in @("sh-services", "sh-datastores")) {
        $d = Get-Json "$GrafanaUrl/api/dashboards/uid/$uid" $auth
        if ($null -eq $d) { Fail "Chua nap dashboard uid=$uid (xem docker logs smart_heritage_grafana)" }
        else { Ok "Dashboard '$($d.dashboard.title)' -> $GrafanaUrl/d/$uid" }
    }
}

# ---------- 5. Swagger chung tai Gateway (Bao W5) ----------
Write-Host "== 5. Swagger chung tai Gateway ($GatewayUrl/swagger-ui.html)"
$cfg = Get-Json "$GatewayUrl/v3/api-docs/swagger-config" $null
if ($null -eq $cfg) {
    if ($RequireServices -contains "api-gateway") { Fail "Gateway khong tra /v3/api-docs/swagger-config" }
    else { Warn "Gateway chua chay hoac chua co Swagger chung - bo qua" }
} else {
    $urls = @($cfg.urls)
    Ok ("Swagger chung co {0} muc chon" -f $urls.Count)
    foreach ($u in $urls) {
        $line = "{0,-24} {1}" -f $u.name, $u.url
        $doc = Get-Json ($GatewayUrl + $u.url) $null
        if ($null -ne $doc -and $doc.openapi) {
            $n = @($doc.paths.PSObject.Properties).Count
            $server = @($doc.servers)[0].url
            if ($server -eq "/") { Ok "$line  $n duong dan API, Try it out goi qua Gateway" }
            else { Fail "$line  servers = $server (phai la / de goi qua Gateway)" }
        }
        elseif ($RequireServices -contains $u.name) { Fail "$line  khong tai duoc tai lieu" }
        else { Warn "$line  khong tai duoc (service chua chay?)" }
    }
}

Write-Host ""
if ($script:fail -gt 0) { Write-Host "KET QUA: $($script:fail) loi, $($script:warn) canh bao" -ForegroundColor Red; exit 1 }
elseif ($script:warn -gt 0) { Write-Host "KET QUA: DAT ($($script:warn) canh bao - xem cac dong [CANH BAO])" -ForegroundColor Yellow; exit 0 }
else { Write-Host "KET QUA: DAT" -ForegroundColor Green; exit 0 }
