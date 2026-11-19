# Kiểm thử hồi quy phần Đánh giá (feedback-service) qua API Gateway – Tường, tuần 7.
# Chạy lại các test case API của Test Plan: TC-36, TC-21, TC-34, TC-18 → TC-20, TC-116, TC-118,
# TC-89 → TC-92, TC-17, TC-35, TC-83. (TC-93 tắt RabbitMQ: làm tay theo Test Plan.)
#
# ĐIỀU KIỆN: DB ở dữ liệu mẫu (vừa down -v rồi up -d), 7 service + Gateway đang chạy, RabbitMQ bật (--profile mq).
# Script CÓ GHI DỮ LIỆU (gửi, duyệt, ẩn đánh giá) nên chỉ chạy 1 lần sau mỗi lần đưa DB về mẫu.
#
# Cách chạy (ở thư mục gốc repo):
#     powershell -ExecutionPolicy Bypass -File Docs/testing/regression-feedback.ps1
# Kết quả: mỗi dòng [ĐẠT] / [LỖI] kèm mã TC; có [LỖI] thì thoát với mã 1.

param(
    [string]$Gateway = "http://localhost:8080",
    [string]$RabbitApi = "http://localhost:15672",
    [string]$RabbitUser = "smartheritage",
    [string]$RabbitPassword = "change-me-rabbit",
    [string]$MysqlContainer = "smart_heritage_mysql",
    [string]$MysqlRootPassword = "rootpassword123"
)

$script:failed = 0
$script:passed = 0
function Check($tc, $name, [bool]$ok, $detail) {
    if ($ok) { $script:passed++; Write-Host ("  [ĐẠT] {0,-7} {1} {2}" -f $tc, $name, $detail) -ForegroundColor Green }
    else { $script:failed++; Write-Host ("  [LỖI] {0,-7} {1} {2}" -f $tc, $name, $detail) -ForegroundColor Red }
}

# Gọi API, trả về @{ Status; Body }. Body gửi dạng byte UTF-8 (PowerShell 5.1 gửi sai dấu tiếng Việt).
function Call($method, $path, $token, $body, $extraHeaders) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    if ($extraHeaders) { foreach ($k in $extraHeaders.Keys) { $headers[$k] = $extraHeaders[$k] } }
    $params = @{ Uri = "$Gateway$path"; Method = $method; UseBasicParsing = $true; TimeoutSec = 15; Headers = $headers }
    if ($null -ne $body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = [Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Compress))
    }
    try {
        $resp = Invoke-WebRequest @params
        $text = [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
        return @{ Status = [int]$resp.StatusCode; Body = ($text | ConvertFrom-Json) }
    }
    catch {
        if ($_.Exception.Response) { return @{ Status = [int]$_.Exception.Response.StatusCode; Body = $null } }
        return @{ Status = 0; Body = $null }
    }
}

function Login($user) {
    $r = Call POST "/api/auth/login" $null @{ username = $user; password = "Test@1234" }
    return $r.Body.accessToken
}

function Ids($result) { return @($result.Body.data | ForEach-Object { $_.id }) -join "," }

# ---- RabbitMQ: queue tạm bind "#" vào sh.events để đếm sự kiện của từng bước ----
$rabbitAuth = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${RabbitUser}:${RabbitPassword}"))
$queue = "qa.regression.feedback"
function Rabbit($method, $path, $body) {
    $params = @{ Uri = "$RabbitApi/api$path"; Method = $method; Headers = @{ Authorization = $rabbitAuth }; UseBasicParsing = $true }
    if ($null -ne $body) { $params.ContentType = "application/json"; $params.Body = ($body | ConvertTo-Json -Compress) }
    $resp = Invoke-WebRequest @params
    $text = [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
    if ($text) { return $text | ConvertFrom-Json }
}
# Lấy (và xoá) mọi tin đang có trong queue tạm.
function Take-Events {
    Start-Sleep -Milliseconds 800
    # Gán vào biến rồi mới duyệt: ConvertFrom-Json của PowerShell 5.1 không tách mảng JSON khi đưa qua pipeline.
    $messages = Rabbit POST "/queues/%2F/$queue/get" @{ count = 50; ackmode = "ack_requeue_false"; encoding = "auto" }
    $list = @()
    foreach ($m in $messages) { $list += [pscustomobject]@{ Key = $m.routing_key; Event = ($m.payload | ConvertFrom-Json) } }
    return ,$list
}

Write-Host "Hồi quy phần Đánh giá qua $Gateway"
$admin = Login "admin"; $mai = Login "visitor_mai"; $an = Login "visitor_an"
if (-not ($admin -and $mai -and $an)) { Write-Host "Không đăng nhập được, dừng." -ForegroundColor Red; exit 1 }

try {
    Rabbit PUT "/queues/%2F/$queue" @{ durable = $false; auto_delete = $false } | Out-Null
    Rabbit POST "/bindings/%2F/e/sh.events/q/$queue" @{ routing_key = "#" } | Out-Null
    $null = Take-Events
}
catch { Write-Host "Không dùng được RabbitMQ API ($RabbitApi): bật --profile mq rồi chạy lại." -ForegroundColor Red; exit 1 }

# TC-36 phải chạy đầu tiên (dữ liệu mẫu chưa bị đổi)
$r = Call GET "/api/artifacts/2/rating-summary" $null
Check "TC-36" "Điểm TB hiện vật 2 (mẫu)" ($r.Status -eq 200 -and $r.Body.data.averageRating -eq 4.0 -and $r.Body.data.totalRatings -eq 1) "(avg=$($r.Body.data.averageRating), total=$($r.Body.data.totalRatings))"

# TC-21: chỉ APPROVED, không lộ userId/email
$r = Call GET "/api/artifacts/1/feedbacks" $null
$leak = ($r.Body.data | ForEach-Object { $_.PSObject.Properties.Name }) -match "^(userId|email)$"
Check "TC-21" "Đánh giá công khai hiện vật 1" ($r.Status -eq 200 -and (Ids $r) -eq "1" -and -not $leak) "(id: $(Ids $r))"

# TC-34: lọc danh sách quản trị
$a = Call GET "/api/admin/feedbacks?status=PENDING" $admin
$b = Call GET "/api/admin/feedbacks?artifactId=1" $admin
$c = Call GET "/api/admin/feedbacks?status=PENDING&artifactId=1" $admin
$sortedB = (@($b.Body.data | ForEach-Object { $_.id }) | Sort-Object) -join ","
$sortedA = (@($a.Body.data | ForEach-Object { $_.id }) | Sort-Object) -join ","
Check "TC-34" "Lọc PENDING / hiện vật 1 / cả hai" ($sortedA -eq "3,5" -and $sortedB -eq "1,3,6" -and (Ids $c) -eq "3") "($sortedA | $sortedB | $(Ids $c))"
$s1 = (Call GET "/api/admin/feedbacks?status=XYZ" $admin).Status
$s2 = (Call GET "/api/admin/feedbacks?artifactId=0" $admin).Status
$s3 = (Call GET "/api/admin/feedbacks" $mai).Status
Check "TC-34" "status sai / artifactId=0 / token visitor" ($s1 -eq 400 -and $s2 -eq 400 -and $s3 -eq 403) "($s1, $s2, $s3)"

# TC-18, TC-19, TC-20: dữ liệu sai không được lưu
$codes = @(@{ rating = 4.5 }, @{ rating = 0 }, @{ rating = 6 }, @{ comment = "thiếu số sao" }) |
    ForEach-Object { (Call POST "/api/artifacts/1/feedbacks" $an $_).Status }
Check "TC-18" "Số sao 4.5 / 0 / 6 / thiếu" (@($codes | Where-Object { $_ -ne 400 }).Count -eq 0) "($($codes -join ', '))"
$r = Call POST "/api/artifacts/1/feedbacks" $an @{ rating = 5; comment = ("a" * 501) }
Check "TC-19" "Bình luận 501 ký tự" ($r.Status -eq 400) "(HTTP $($r.Status))"
$r = Call POST "/api/artifacts/99/feedbacks" $an @{ rating = 5 }
Check "TC-20" "Hiện vật không tồn tại" ($r.Status -eq 404) "(HTTP $($r.Status))"
$events = Take-Events
Check "TC-18/20" "Dữ liệu sai không phát sự kiện" ($events.Count -eq 0) "($($events.Count) tin)"

# TC-116: đánh giá của tôi, header X-User-Id giả bị bỏ qua
$r = Call GET "/api/feedbacks/me" $mai $null @{ "X-User-Id" = "2" }
$noToken = (Call GET "/api/feedbacks/me" $null).Status
Check "TC-116" "Đánh giá của tôi (visitor_mai, header giả user 2)" ($r.Status -eq 200 -and ((@($r.Body.data | ForEach-Object { $_.id }) | Sort-Object) -join ",") -eq "3,5" -and $noToken -eq 401) "(id: $(Ids $r); không token: $noToken)"

# TC-118: phân quyền số chờ duyệt
$r = Call GET "/api/admin/feedbacks/pending-count" $admin
$v = (Call GET "/api/admin/feedbacks/pending-count" $mai).Status
$n = (Call GET "/api/admin/feedbacks/pending-count" $null).Status
Check "TC-118" "pending-count admin / visitor / không token" ($r.Status -eq 200 -and $r.Body.data.pending -eq 2 -and $v -eq 403 -and $n -eq 401) "(pending=$($r.Body.data.pending); $v; $n)"

# TC-89: đánh giá mới -> đúng 1 tin feedback.created
$r = Call POST "/api/artifacts/3/feedbacks" $mai @{ rating = 4; comment = "Rất hay" }
$events = Take-Events
$created = @($events | Where-Object Key -eq "feedback.created")
$d = if ($created.Count) { $created[0].Event.data } else { $null }
Check "TC-89" "Đánh giá mới phát FeedbackCreated" ($r.Status -eq 201 -and $created.Count -eq 1 -and $events.Count -eq 1 -and
    $created[0].Event.type -eq "FeedbackCreated" -and $d.userId -eq 3 -and $d.artifactId -eq 3 -and $d.rating -eq 4) "(HTTP $($r.Status), $($events.Count) tin)"

# TC-90: gửi lại = sửa, không phát sự kiện
$r = Call POST "/api/artifacts/3/feedbacks" $mai @{ rating = 2 }
$events = Take-Events
Check "TC-90" "Gửi lại không phát FeedbackCreated" ($r.Status -eq 200 -and $r.Body.data.status -eq "PENDING" -and $events.Count -eq 0) "(HTTP $($r.Status), $($events.Count) tin)"

# TC-17: visitor_mai gửi cho hiện vật 2 (đã có id 5) -> 200, ghi đè id 5, reviewerName đúng
$r = Call POST "/api/artifacts/2/feedbacks" $mai @{ rating = 4; comment = "Rất hay" }
Check "TC-17" "Gửi lại hiện vật 2 ghi đè id 5" ($r.Status -eq 200 -and $r.Body.data.id -eq 5 -and $r.Body.data.status -eq "PENDING" -and
    $r.Body.data.reviewerName -eq "Trần Thị Mai") "(HTTP $($r.Status), id $($r.Body.data.id))"
$null = Take-Events

# TC-91: duyệt id 3 -> đúng 1 tin feedback.approved có tên hiện vật 1
$r = Call PATCH "/api/admin/feedbacks/3/status" $admin @{ status = "APPROVED" }
$events = Take-Events
$approved = @($events | Where-Object Key -eq "feedback.approved")
$d = if ($approved.Count) { $approved[0].Event.data } else { $null }
Check "TC-91" "Duyệt phát FeedbackApproved" ($r.Status -eq 200 -and $approved.Count -eq 1 -and $events.Count -eq 1 -and
    $d.feedbackId -eq 3 -and $d.userId -eq 3 -and $d.artifactId -eq 1 -and $d.artifactName) "(artifactName: $($d.artifactName))"

# TC-92: duyệt lại id 3, ẩn id 5 -> không có tin mới
$r1 = (Call PATCH "/api/admin/feedbacks/3/status" $admin @{ status = "APPROVED" }).Status
$r2 = (Call PATCH "/api/admin/feedbacks/5/status" $admin @{ status = "REJECTED" }).Status
$events = Take-Events
Check "TC-92" "Duyệt lại / ẩn không báo lặp" ($r1 -eq 200 -and $r2 -eq 200 -and $events.Count -eq 0) "($r1, $r2, $($events.Count) tin)"

# TC-35: duyệt rồi ẩn id của TC-89 (hiện vật 3), PENDING -> 400, id không tồn tại -> 404
$newId = (Call GET "/api/admin/feedbacks?status=PENDING&artifactId=3" $admin).Body.data[0].id
$before = (Call GET "/api/artifacts/3/feedbacks" $null)
$ok1 = (Call PATCH "/api/admin/feedbacks/$newId/status" $admin @{ status = "APPROVED" }).Status
$shown = (Ids (Call GET "/api/artifacts/3/feedbacks" $null)) -split "," -contains "$newId"
$ok2 = (Call PATCH "/api/admin/feedbacks/$newId/status" $admin @{ status = "REJECTED" }).Status
$hidden = -not ((Ids (Call GET "/api/artifacts/3/feedbacks" $null)) -split "," -contains "$newId")
$bad = (Call PATCH "/api/admin/feedbacks/$newId/status" $admin @{ status = "PENDING" }).Status
$missing = (Call PATCH "/api/admin/feedbacks/99999/status" $admin @{ status = "APPROVED" }).Status
Check "TC-35" "Duyệt hiện / ẩn biến mất / PENDING 400 / id sai 404" ($ok1 -eq 200 -and $shown -and $ok2 -eq 200 -and $hidden -and $bad -eq 400 -and $missing -eq 404) "(id ${newId}: $ok1, $ok2, $bad, $missing)"
$null = Take-Events

# TC-83: khoá duy nhất (user_id, artifact_id) ở mức database
$sql = "INSERT INTO feedback_db.feedbacks (user_id, artifact_id, rating, status) VALUES (2, 1, 5, 'PENDING')"
$out = docker exec $MysqlContainer mysql -uroot "-p$MysqlRootPassword" -e $sql 2>&1 | Out-String
Check "TC-83" "Chèn trùng (user 2, hiện vật 1) bị chặn" ($out -match "1062" -and $out -match "uq_feedbacks_user_artifact") "(MySQL báo lỗi 1062)"

# Dọn queue tạm
try { Rabbit DELETE "/queues/%2F/$queue" | Out-Null } catch { }

Write-Host ("Kết quả: {0} đạt, {1} lỗi" -f $script:passed, $script:failed) -ForegroundColor $(if ($script:failed) { "Red" } else { "Green" })
if ($script:failed -gt 0) { exit 1 }
