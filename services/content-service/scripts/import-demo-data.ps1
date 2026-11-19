param([string]$BaseUrl = 'http://localhost:8080', [string]$DemoDirectory = (Join-Path $PSScriptRoot '../demo-data'))
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath($DemoDirectory)
$manifest = [IO.File]::ReadAllText((Join-Path $root 'manifest.json')) | ConvertFrom-Json
# Preflight is deliberately before all API writes: never claim a complete demo with missing assets.
$missing = @()
foreach ($artifact in $manifest.artifacts) {
    foreach ($suffix in @('.jpg', '-thumb.jpg', '-en.wav')) {
        $file = Join-Path $root ('media/' + $artifact.key + $suffix)
        if (-not (Test-Path $file)) { $missing += $file }
    }
}
if ($missing.Count) { throw ('Missing demo media; no API writes performed: ' + ($missing -join ', ')) }
Add-Type -AssemblyName System.Net.Http
$client = [Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(45)
$headers = @{}
function Api([string]$Method, [string]$Path, $Data = $null) {
    $args = @{Uri=$BaseUrl.TrimEnd('/')+$Path;Method=$Method;Headers=$headers;TimeoutSec=30}
    if ($null -ne $Data) {
        $args.ContentType = 'application/json; charset=utf-8'
        $args.Body = [Text.Encoding]::UTF8.GetBytes(($Data | ConvertTo-Json -Depth 20))
    }
    Invoke-RestMethod @args
}
function Upload([string]$Path, [string]$ContentType) {
    $form = [Net.Http.MultipartFormDataContent]::new()
    try {
        $part = [Net.Http.ByteArrayContent]::new([IO.File]::ReadAllBytes($Path))
        $part.Headers.ContentType = [Net.Http.Headers.MediaTypeHeaderValue]::new($ContentType)
        $form.Add($part, 'file', [IO.Path]::GetFileName($Path))
        $response = $client.PostAsync($BaseUrl.TrimEnd('/')+'/api/media/upload', $form).GetAwaiter().GetResult()
        try {
            $null = $response.EnsureSuccessStatusCode()
            $result = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
            return $result.data.fileUrl
        } finally { $response.Dispose() }
    } finally { $form.Dispose() }
}
try {
    $login = Api POST '/api/auth/login' @{username='admin';password='Test@1234'}
    if (-not $login.accessToken) { throw 'Admin login failed' }
    $headers.Authorization = 'Bearer ' + $login.accessToken
    $client.DefaultRequestHeaders.Authorization = [Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $login.accessToken)
    $sites = (Api GET '/api/heritage-sites').data
    foreach ($site in $manifest.sites) {
        $foundSite = $sites | Where-Object id -eq $site.existingSiteId | Select-Object -First 1
        if (-not $foundSite) { throw ('Required demo site missing: ' + $site.name) }
        $expectedName = $site.name.Normalize([Text.NormalizationForm]::FormD).ToLowerInvariant() -replace '\p{M}|[^a-z0-9]', ''
        $actualName = $foundSite.name.Normalize([Text.NormalizationForm]::FormD).ToLowerInvariant() -replace '\p{M}|[^a-z0-9]', ''
        if ($actualName -ne $expectedName) { throw ('Site ID belongs to a different site; check manifest mapping before importing: ' + $site.existingSiteId) }
    }
    $existing = @((Api GET '/api/artifacts').data)
    $imported = @()
    foreach ($artifact in $manifest.artifacts) {
        $site = $manifest.sites | Where-Object key -eq $artifact.siteKey
        $image = Upload (Join-Path $root ('media/'+$artifact.key+'.jpg')) 'image/jpeg'
        $thumb = Upload (Join-Path $root ('media/'+$artifact.key+'-thumb.jpg')) 'image/jpeg'
        $audio = Upload (Join-Path $root ('media/'+$artifact.key+'-en.wav')) 'audio/wav'
        $images = ConvertTo-Json -InputObject @($image) -Compress
        $payload = @{
            siteId=$site.existingSiteId; name=$artifact.name; artifactCode=$artifact.artifactCode
            description=$artifact.vi; thumbnailUrl=$thumb; status='ACTIVE'
            contents=@(
                @{languageCode='vi';title=$artifact.name;description=$artifact.vi;imageUrls=$images},
                @{languageCode='en';title=$artifact.enTitle;description=$artifact.en;imageUrls=$images;audioUrl=$audio;transcript=$artifact.en}
            )
            media=@(@{mediaType='IMAGE';fileUrl=$image;caption=$artifact.photoCaption})
        }
        $found = $existing | Where-Object artifactCode -eq $artifact.artifactCode | Select-Object -First 1
        $result = if ($found) { Api PUT ('/api/artifacts/'+$found.id) $payload } else { Api POST '/api/artifacts' $payload }
        $imported += [ordered]@{artifactId=$result.data.id;artifactCode=$artifact.artifactCode;siteId=$site.existingSiteId;imageUrl=$image;thumbnailUrl=$thumb;englishAudioUrl=$audio}
        Write-Output ('Imported ' + $artifact.artifactCode + ' id=' + $result.data.id)
    }
    [IO.File]::WriteAllText((Join-Path $root 'imported-records.json'), ($imported | ConvertTo-Json -Depth 8))
} finally { $client.Dispose() }
