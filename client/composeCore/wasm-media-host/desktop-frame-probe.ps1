# This is a private project. All rights reserved.
[CmdletBinding(PositionalBinding=$false)]
param([int]$BrowserProcess,[int]$SourceWidth,[switch]$NoSampling)
$ErrorActionPreference='Stop'
Add-Type -Path (Join-Path $PSScriptRoot 'DesktopFrameProbe.cs')
[DesktopFrameProbe]::WaitForForeground($BrowserProcess,180)
$bounds=[DesktopFrameProbe]::Calibrate($BrowserProcess)
@{ready=$true;bounds=$bounds} | ConvertTo-Json -Compress
try {
    if ([Console]::ReadLine() -ne 'start') {throw 'Expected start command'}
    [DesktopFrameProbe]::Start($bounds,$BrowserProcess,$SourceWidth,(-not $NoSampling))
    @{started=$true} | ConvertTo-Json -Compress
    if ([Console]::ReadLine() -ne 'stop') {throw 'Expected stop command'}
    [DesktopFrameProbe]::Stop() | ConvertTo-Json -Depth 5 -Compress
} finally {
    [void][DesktopFrameProbe]::Stop()
}
