param([string]$DemoDirectory = (Join-Path $PSScriptRoot '../demo-data'))
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = [IO.Path]::GetFullPath($DemoDirectory)
$manifest = [IO.File]::ReadAllText((Join-Path $root 'manifest.json')) | ConvertFrom-Json
$encoder = [Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object MimeType -eq 'image/jpeg'
foreach ($item in $manifest.artifacts) {
    $path = Join-Path $root ('media/' + $item.key + '.jpg')
    if (-not (Test-Path $path)) { continue }
    $bytes = [IO.File]::ReadAllBytes($path)
    if ($bytes.Length -lt 2 -or $bytes[0] -ne 255 -or $bytes[1] -ne 216) { continue }
    $image = [Drawing.Image]::FromFile($path)
    $width = [Math]::Min(320, $image.Width)
    $height = [Math]::Max(1, [int]($image.Height * $width / $image.Width))
    $thumb = [Drawing.Bitmap]::new($width, $height)
    $graphics = [Drawing.Graphics]::FromImage($thumb)
    $quality = [Drawing.Imaging.EncoderParameters]::new(1)
    $quality.Param[0] = [Drawing.Imaging.EncoderParameter]::new([Drawing.Imaging.Encoder]::Quality, [long]75)
    try {
        $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.DrawImage($image, 0, 0, $width, $height)
        $output = Join-Path $root ('media/' + $item.key + '-thumb.jpg')
        $thumb.Save($output, $encoder, $quality)
        Write-Output ($item.key + ': ' + $bytes.Length + ' -> ' + (Get-Item $output).Length + ' bytes')
    } finally { $quality.Dispose(); $graphics.Dispose(); $thumb.Dispose(); $image.Dispose() }
}
