param([string]$DemoDirectory = (Join-Path $PSScriptRoot '../demo-data'), [switch]$AudioOnly)
$ErrorActionPreference = 'Stop'
$demoRoot = [IO.Path]::GetFullPath($DemoDirectory)
$manifest = [IO.File]::ReadAllText((Join-Path $demoRoot 'manifest.json')) | ConvertFrom-Json
$media = Join-Path $demoRoot 'media'
$null = New-Item -ItemType Directory -Path $media -Force
Add-Type -AssemblyName System.Speech
$speech = [System.Speech.Synthesis.SpeechSynthesizer]::new()
$speech.SelectVoice('Microsoft Zira Desktop')
$speech.Rate = -1
$credits = @()
try {
    foreach ($item in $manifest.artifacts) {
        if ($AudioOnly) {
            $speech.SetOutputToWaveFile((Join-Path $media ($item.key + '-en.wav')))
            $speech.Speak($item.en)
            $speech.SetOutputToNull()
            Write-Output ('Prepared English audio ' + $item.key)
            continue
        }
        $query = 'https://commons.wikimedia.org/w/api.php?action=query&format=json&prop=imageinfo&iiprop=url%7Csize%7Cextmetadata&iiurlwidth=960&titles=' + [uri]::EscapeDataString($item.commonsFile)
        $metadata = Invoke-RestMethod $query -Headers @{'User-Agent'='SmartHeritage-course-demo/1.0'} -TimeoutSec 30
        $info = $metadata.query.pages.PSObject.Properties.Value.imageinfo | Select-Object -First 1
        if (-not $info.url) { throw "Image not found: $($item.commonsFile)" }
        $imageUrl = if ($info.width -gt 960 -and $info.thumburl) { $info.thumburl } else { $info.url }
        $imagePath = Join-Path $media ($item.key + '.jpg')
        $existingBytes = if (Test-Path $imagePath) { [IO.File]::ReadAllBytes($imagePath) } else { @() }
        if ($existingBytes.Length -lt 2 -or $existingBytes[0] -ne 255 -or $existingBytes[1] -ne 216) {
            for ($attempt = 1; $attempt -le 3; $attempt++) {
                try {
                    Invoke-WebRequest $imageUrl -UseBasicParsing -OutFile $imagePath -TimeoutSec 45
                    break
                } catch {
                    if ([int]$_.Exception.Response.StatusCode -ne 429 -or $attempt -eq 3) { throw }
                    Write-Output ('Rate limited; waiting 30 seconds before retry ' + $item.key)
                    Start-Sleep -Seconds 30
                }
            }
        }
        $license = $info.extmetadata.LicenseShortName.value
        if (-not $license) { throw "Missing image license: $($item.commonsFile)" }
        $credits += [ordered]@{
            file = 'media/' + $item.key + '.jpg'; commonsFile = $item.commonsFile
            source = 'https://commons.wikimedia.org/wiki/' + [uri]::EscapeDataString($item.commonsFile)
            originalUrl = $info.url; downloadedUrl = $imageUrl
            author = [regex]::Replace($info.extmetadata.Artist.value, '<[^>]+>', '')
            license = $license; licenseUrl = $info.extmetadata.LicenseUrl.value
            thumbnail = 'media/' + $item.key + '-thumb.jpg'
            modifications = 'Wikimedia preview; local 320px JPEG thumbnail at quality 75'
            audio = 'media/' + $item.key + '-en.wav'; audioCreator = 'Microsoft Zira Desktop, local synthesized English demo narration'
        }
        $speech.SetOutputToWaveFile((Join-Path $media ($item.key + '-en.wav')))
        $speech.Speak($item.en)
        $speech.SetOutputToNull()
        [IO.File]::WriteAllText((Join-Path $demoRoot 'media-credits.json'), ($credits | ConvertTo-Json -Depth 8))
        Write-Output ('Prepared ' + $item.key + ' (' + $license + ')')
        Start-Sleep -Seconds 2
    }
    if (-not $AudioOnly) { [IO.File]::WriteAllText((Join-Path $demoRoot 'media-credits.json'), ($credits | ConvertTo-Json -Depth 8)) }
} finally { $speech.Dispose() }
