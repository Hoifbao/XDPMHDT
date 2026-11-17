# Kiểm thử tích hợp (Integration) qua API Gateway – Tường, tuần 7. Dùng cho Test Report và kiểm thử hồi quy.
#
# Cách chạy (hệ thống đang chạy, ở thư mục gốc repo):
#     powershell -ExecutionPolicy Bypass -File Docs/testing/integration-smoke.ps1
#     powershell -ExecutionPolicy Bypass -File Docs/testing/integration-smoke.ps1 -Events
#
# Mặc định chỉ ĐỌC dữ liệu: mỗi service 1 API chính qua Gateway + phân quyền 401/403.
# -Events: thêm 1 đánh giá mới của visitor_john cho hiện vật 2 (có ghi dữ liệu), rồi kiểm tra sự kiện
#          feedback.created đi qua RabbitMQ tới analytics-service (số "feedbacks" hôm nay tăng 1).
#          Chỉ chạy được 1 lần sau khi đưa DB về dữ liệu mẫu (lần sau là sửa đánh giá cũ, không phát sự kiện).
# Kết quả: mỗi dòng [ĐẠT] / [LỖI] / [BỎ QUA]; có [LỖI] thì thoát với mã 1.

param(
    [string]$Gateway = "http://localhost:8080",
    [switch]$Events
)

$script:failed = 0
function Pass($name, $detail) { Write-Host ("  [ĐẠT]    {0} {1}" -f $name, $detail) -ForegroundColor Green }
function Fail($name, $detail) { Write-Host ("  [LỖI]    {0} {1}" -f $name, $detail) -ForegroundColor Red; $script:failed++ }
function Skip($name, $detail) { Write-Host ("  [BỎ QUA] {0} {1}" -f $name, $detail) -ForegroundColor Yellow }

# Gọi API, trả về @{ Status = mã HTTP; Body = object JSON hoặc $null }. Không ném lỗi khi 4xx/5xx.
function Call($method, $path, $token, $body) {
    $params = @{ Uri = "$Gateway$path"; Method = $method; UseBasicParsing = $true; TimeoutSec = 15 }
    if ($token) { $params.Headers = @{ Authorization = "Bearer $token" } }
    if ($body) {
        # Gửi byte UTF-8: PowerShell 5.1 gửi chuỗi có dấu tiếng Việt sai bảng mã, service trả 400.
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = [Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json))
    }
    try {
        $resp = Invoke-WebRequest @params
        $text = [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
        $json = $null
        if ($text) { try { $json = $text | ConvertFrom-Json } catch { } }
        return @{ Status = [int]$resp.StatusCode; Body = $json }
    }
    catch {
        if ($_.Exception.Response) { return @{ Status = [int]$_.Exception.Response.StatusCode; Body = $null } }
        return @{ Status = 0; Body = $null }   # không kết nối được
    }
}

function Expect($name, $result, [int[]]$expected) {
    if ($expected -contains $result.Status) { Pass $name "(HTTP $($result.Status))" }
    else { Fail $name "(HTTP $($result.Status), mong đợi $($expected -join '/'))" }
}

function Login($user) {
    $r = Call POST "/api/auth/login" $null @{ username = $user; password = "Test@1234" }
    if ($r.Status -eq 200 -and $r.Body.accessToken) { return $r.Body.accessToken }
    return $null
}

Write-Host "Kiểm thử tích hợp qua $Gateway"

# 1. Đăng nhập (auth-service)
$admin = Login "admin"
$visitor = Login "visitor_an"
if ($admin -and $visitor) { Pass "auth-service: đăng nhập admin, visitor_an" "" }
else { Fail "auth-service: đăng nhập admin, visitor_an" "(không lấy được token, dừng)"; exit 1 }

# 2. Mỗi service 1 API chính, đi qua Gateway
Expect "site-service: GET /api/heritage-sites"                  (Call GET "/api/heritage-sites" $null) @(200)
Expect "content-service: GET /api/artifacts/1"                  (Call GET "/api/artifacts/1" $null) @(200)
Expect "beacon-service: GET /api/ibeacons/registry"             (Call GET "/api/ibeacons/registry" $null) @(200)
Expect "personalization-service: GET /api/me/favorites"         (Call GET "/api/me/favorites?page=0&size=1" $visitor) @(200)
Expect "feedback-service: GET /api/artifacts/1/rating-summary"  (Call GET "/api/artifacts/1/rating-summary" $null) @(200)
Expect "analytics-service: GET /api/statistics/overview"        (Call GET "/api/statistics/overview" $admin) @(200)
Expect "auth-service: GET /api/admin/users (ADMIN)"             (Call GET "/api/admin/users?page=0&size=5" $admin) @(200)

# 3. Phân quyền tại Gateway
Expect "Gateway: VISITOR gọi /api/admin/users"                  (Call GET "/api/admin/users" $visitor) @(403)
Expect "Gateway: không token gọi /api/admin/users"              (Call GET "/api/admin/users" $null) @(401)
Expect "Gateway: VISITOR sửa di tích"                           (Call PUT "/api/heritage-sites/1" $visitor @{ name = "x" }) @(403)

# 4. Luồng sự kiện RabbitMQ (tuỳ chọn, có ghi dữ liệu)
if ($Events) {
    $name = "RabbitMQ: feedback.created -> analytics (số đánh giá hôm nay)"
    $john = Login "visitor_john"
    $before = (Call GET "/api/statistics/realtime" $admin).Body.data.feedbacks
    $sent = Call POST "/api/artifacts/2/feedbacks" $john @{ rating = 5; comment = "Kiểm thử tích hợp" }
    if ($sent.Status -eq 200) {
        Skip $name "(visitor_john đã có đánh giá cho hiện vật 2 nên chỉ sửa, không phát sự kiện; đưa DB về mẫu rồi chạy lại)"
    }
    elseif ($sent.Status -ne 201) {
        Fail $name "(gửi đánh giá trả HTTP $($sent.Status))"
    }
    else {
        $after = $before
        for ($i = 0; $i -lt 10 -and $after -le $before; $i++) {
            Start-Sleep -Seconds 1
            $after = (Call GET "/api/statistics/realtime" $admin).Body.data.feedbacks
        }
        if ($after -gt $before) { Pass $name "($before -> $after)" }
        else { Fail $name "(sau 10 giây vẫn là $after; kiểm tra RabbitMQ đã bật --profile mq)" }
    }
}
else {
    Skip "RabbitMQ: feedback.created -> analytics" "(thêm -Events để chạy, có ghi dữ liệu)"
}

if ($script:failed -gt 0) { Write-Host "Kết quả: $($script:failed) mục LỖI" -ForegroundColor Red; exit 1 }
Write-Host "Kết quả: tất cả đạt" -ForegroundColor Green
