param([string]$BaseUrl = 'http://localhost:8080')

# Creates only a uniquely named acceptance artifact and deletes it in finally.
# Does not edit existing artifacts or claim RabbitMQ/consumer verification.
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$artifactId = $null
$headers = @{}
function Api([string]$Method, [string]$Path, $Payload = $null) {
    $args = @{ Uri = "$BaseUrl$Path"; Method = $Method; Headers = $headers; TimeoutSec = 20 }
    if ($null -ne $Payload) {
        $args.ContentType = 'application/json; charset=utf-8'
        $args.Body = [Text.Encoding]::UTF8.GetBytes(($Payload | ConvertTo-Json -Depth 15))
    }
    Invoke-RestMethod @args
}
try {
    $login = Api POST '/api/auth/login' @{ username = 'admin'; password = 'Test@1234' }
    if (-not $login.accessToken) { throw 'Login did not return accessToken' }
    $headers.Authorization = "Bearer $($login.accessToken)"
    $swagger = Invoke-WebRequest "$BaseUrl/swagger-ui.html" -UseBasicParsing -TimeoutSec 20
    if ($swagger.StatusCode -ne 200) { throw 'Swagger unavailable' }
    $spec = Api GET '/v3/api-docs/content'
    foreach ($path in @('/api/artifacts', '/api/artifacts/{id}', '/api/artifacts/{id}/contents', '/api/artifacts/{artifactId}/media')) {
        if (-not $spec.paths.PSObject.Properties[$path]) { throw "Missing Swagger API: $path" }
    }
    Write-Output 'OK: admin login and content APIs in Gateway Swagger'
    $sites = @((Api GET '/api/heritage-sites').data)
    if (-not $sites.Count -or -not $sites[0].id) { throw 'Need at least one heritage site' }
    $unique = 'XA-W6-' + [Guid]::NewGuid().ToString('N').Substring(0, 12)
    $created = Api POST '/api/artifacts' @{
        siteId = $sites[0].id; name = $unique; artifactCode = $unique
        contents = @(@{ languageCode = 'vi'; title = $unique; description = 'W6 acceptance fixture' })
        media = @()
    }
    $artifactId = $created.data.id
    if (-not $artifactId) { throw 'Create did not return artifact ID' }
    Write-Output "OK: created acceptance artifact $artifactId"
    $null = Api PUT "/api/artifacts/$artifactId" @{ name = "$unique-updated"; beaconUuid = "$unique-beacon" }
    $en = Api POST "/api/artifacts/$artifactId/contents" @{ languageCode = 'en'; title = 'W6 English'; description = 'English acceptance fixture' }
    $detail = Api GET "/api/artifacts/${artifactId}?lang=en"
    if ($detail.data.content.title -ne 'W6 English') { throw 'English translation mismatch' }
    $null = Api DELETE "/api/artifacts/contents/$($en.data.id)"
    $resource = Api POST "/api/artifacts/$artifactId/media" @{ mediaType = 'IMAGE'; fileUrl = 'https://example.com/w6.png' }
    $null = Api DELETE "/api/media/$($resource.data.id)"
    $fallback = Api GET "/api/artifacts/${artifactId}?lang=en"
    if (-not $fallback.data.fallbackUsed) { throw 'Missing Vietnamese fallback' }
    Write-Output 'OK: artifact update, translation add/delete, media add/delete and fallback'
} finally {
    if ($artifactId) {
        $null = Api DELETE "/api/artifacts/$artifactId"
        Write-Output "OK: removed acceptance artifact $artifactId"
    }
}
