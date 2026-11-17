# Xuất tài liệu OpenAPI (Swagger) của 7 service qua API Gateway vào thư mục này.
#
# Cách chạy (ở thư mục gốc repo, hệ thống đang chạy, Gateway ở cổng 8080):
#     powershell -ExecutionPolicy Bypass -File Docs/api/openapi/export-openapi.ps1
#     powershell -ExecutionPolicy Bypass -File Docs/api/openapi/export-openapi.ps1 -Gateway http://localhost:8080
#
# Mỗi service ra 1 file <service>.json (UTF-8, đã thụt lề). Service nào không trả được thì báo lỗi
# và chạy tiếp các service còn lại; cuối cùng thoát với mã 1 nếu có service lỗi.
# Gateway lấy /v3/api-docs/<tên> từ service tương ứng (xem springdoc trong services/api-gateway/.../application.yml),
# nên đường dẫn trong file là đường dẫn client gọi qua Gateway.

param(
    [string]$Gateway = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

# Tên nhóm trên Gateway => tên file
$services = [ordered]@{
    "auth"            = "auth-service"
    "site"            = "site-service"
    "content"         = "content-service"
    "beacon"          = "beacon-service"
    "personalization" = "personalization-service"
    "feedback"        = "feedback-service"
    "analytics"       = "analytics-service"
}

# Thụt lề chuỗi JSON (giữ nguyên thứ tự khoá và ký tự tiếng Việt; ConvertTo-Json của PowerShell 5.1 làm hỏng cả hai).
function Format-Json([string]$json) {
    $sb = New-Object System.Text.StringBuilder
    $indent = 0
    $inString = $false
    $escaped = $false
    foreach ($ch in $json.ToCharArray()) {
        if ($inString) {
            [void]$sb.Append($ch)
            if ($escaped) { $escaped = $false }
            elseif ($ch -eq '\') { $escaped = $true }
            elseif ($ch -eq '"') { $inString = $false }
            continue
        }
        switch ($ch) {
            '"' { $inString = $true; [void]$sb.Append($ch) }
            { $_ -eq '{' -or $_ -eq '[' } {
                $indent++
                [void]$sb.Append($ch).Append("`n").Append('  ' * $indent)
            }
            { $_ -eq '}' -or $_ -eq ']' } {
                $indent--
                [void]$sb.Append("`n").Append('  ' * $indent).Append($ch)
            }
            ',' { [void]$sb.Append($ch).Append("`n").Append('  ' * $indent) }
            ':' { [void]$sb.Append(': ') }
            { $_ -match '\s' } { }
            default { [void]$sb.Append($ch) }
        }
    }
    # Bỏ dòng trống của object/mảng rỗng: "{\n  }" -> "{}"
    $text = $sb.ToString() -replace '\{\s+\}', '{}' -replace '\[\s+\]', '[]'
    return $text + "`n"
}

$outDir = $PSScriptRoot
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$failed = @()

foreach ($group in $services.Keys) {
    $file = Join-Path $outDir ($services[$group] + ".json")
    $url = "$Gateway/v3/api-docs/$group"
    try {
        $response = Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 30
        # Đọc byte rồi tự giải mã UTF-8, tránh PowerShell 5.1 đoán sai bảng mã làm lỗi dấu tiếng Việt.
        $bytes = $response.RawContentStream.ToArray()
        $json = [System.Text.Encoding]::UTF8.GetString($bytes)
        $paths = ($json | ConvertFrom-Json).paths.PSObject.Properties.Name.Count
        [System.IO.File]::WriteAllText($file, (Format-Json $json), $utf8NoBom)
        Write-Host ("OK    {0,-24} {1,3} đường dẫn -> {2}" -f $services[$group], $paths, (Split-Path $file -Leaf))
    }
    catch {
        $failed += $services[$group]
        Write-Host ("LỖI   {0,-24} {1}: {2}" -f $services[$group], $url, $_.Exception.Message) -ForegroundColor Red
    }
}

if ($failed.Count -gt 0) {
    Write-Host "Không xuất được: $($failed -join ', '). Kiểm tra service đó và Gateway đã chạy chưa." -ForegroundColor Red
    exit 1
}
Write-Host "Xong: đã xuất $($services.Count) file vào $outDir"
