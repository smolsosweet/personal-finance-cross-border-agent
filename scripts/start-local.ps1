[CmdletBinding()]
param(
    [ValidateRange(1,65535)][int]$Port = 8080,
    [ValidatePattern('^gemini-[A-Za-z0-9._-]+$')][string]$Model = 'gemini-3.5-flash-lite',
    [switch]$OpenBrowser,
    [ValidateRange(10,600)][int]$StartupTimeoutSeconds = 120,
    [ValidateRange(1,120)][int]$ConnectTimeoutSeconds = 3,
    [ValidateRange(1,120)][int]$RequestTimeoutSeconds = 30
)

$ErrorActionPreference = 'Stop'
$keyFile = Join-Path $env:LOCALAPPDATA 'FinBridge\gemini-api-key.dpapi'
$secureKey = $null
$apiKey = $env:GEMINI_API_KEY

if ([string]::IsNullOrWhiteSpace($apiKey)) {
    if (Test-Path -LiteralPath $keyFile) {
        try {
            $secureKey = ConvertTo-SecureString -String ([System.IO.File]::ReadAllText($keyFile).Trim())
            $apiKey = [System.Net.NetworkCredential]::new('', $secureKey).Password
            Write-Host 'Loaded your Windows-encrypted local Gemini key.'
        } catch {
            $secureKey = $null
            $apiKey = $null
            Write-Warning 'Could not decrypt the saved Gemini key. Enter it again.'
        }
    }
    if ([string]::IsNullOrWhiteSpace($apiKey)) {
        $secureKey = Read-Host 'Gemini API key (hidden; saved encrypted for this Windows account)' -AsSecureString
        if ($secureKey.Length -eq 0) { throw 'Gemini API key is required to start FinBridge.' }
        $apiKey = [System.Net.NetworkCredential]::new('', $secureKey).Password
        $keyDirectory = Split-Path -Parent $keyFile
        New-Item -ItemType Directory -Path $keyDirectory -Force | Out-Null
        ConvertFrom-SecureString -SecureString $secureKey | Set-Content -LiteralPath $keyFile -NoNewline
        Write-Host 'Saved the key encrypted for this Windows account.'
    }
}

$env:GEMINI_API_KEY = $apiKey
$launcher = Join-Path $PSScriptRoot 'start-gemini.ps1'
if (-not (Test-Path -LiteralPath $launcher)) { throw "Gemini launcher not found: $launcher" }
& $launcher -Port $Port -Model $Model -OpenBrowser:$OpenBrowser -StartupTimeoutSeconds $StartupTimeoutSeconds -ConnectTimeoutSeconds $ConnectTimeoutSeconds -RequestTimeoutSeconds $RequestTimeoutSeconds
exit $LASTEXITCODE
