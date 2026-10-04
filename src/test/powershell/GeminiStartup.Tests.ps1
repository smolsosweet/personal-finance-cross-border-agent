param([string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path)
$ErrorActionPreference='Stop'
. (Join-Path $ProjectRoot 'scripts/start-gemini.ps1')
$script:Checks=0
function Check([bool]$Value,[string]$Name) {
    if(-not $Value){throw "FAIL: $Name"};$script:Checks++;Write-Output "PASS: $Name"
}
$errors=$null
[void][System.Management.Automation.Language.Parser]::ParseFile((Join-Path $ProjectRoot 'scripts/start-gemini.ps1'),[ref]$null,[ref]$errors)
Check ($errors.Count -eq 0) 'PS 5.1 syntax'
$priorKey=$env:GEMINI_API_KEY;$priorProvider=$env:FINBRIDGE_LLM_PROVIDER;$priorModel=$env:FINBRIDGE_LLM_MODEL;$cwd=(Get-Location).Path
try {
    $env:GEMINI_API_KEY=''
    $missing=Invoke-FinBridgeGemini 8121 'gemini-3.5-flash-lite' $false 10 3 30 6>&1
    Check ($missing[-1] -eq 1 -and ($missing|Out-String) -like '*GEMINI_API_KEY is missing*') 'Missing inherited key fails clearly'
    $env:GEMINI_API_KEY='synthetic-launcher-key';$env:FINBRIDGE_LLM_PROVIDER='ollama';$env:FINBRIDGE_LLM_MODEL='qwen3:4b'
    $info=New-FinBridgeGeminiProcessInfo $ProjectRoot 'C:\Synthetic Path\mvn.cmd' 8121 'gemini-3.5-flash-lite' 4 20
    Check ($info.Arguments -like '*--spring.profiles.active=gemini*' -and $info.Arguments -like '*--server.port=8121*') 'Explicit Gemini profile and port'
    Check ($info.EnvironmentVariables['FINBRIDGE_LLM_PROVIDER'] -eq 'gemini' -and $info.EnvironmentVariables['FINBRIDGE_LLM_MODEL'] -eq 'gemini-3.5-flash-lite') 'Child provider/model overrides stale Ollama environment'
    Check ($info.EnvironmentVariables['GEMINI_API_KEY'] -eq $env:GEMINI_API_KEY -and $info.Arguments -notlike '*synthetic-launcher-key*') 'Key inherited only, absent from command line'
    Check ($info.Arguments -like '*--finbridge.llm.request-timeout=20s*' -and $info.CreateNoWindow) 'Configurable timeout and hidden child'
    function Assert-FinBridgeOllama {throw 'Ollama must never be called'}
    function Invoke-RestMethod {throw 'No model or Ollama HTTP call allowed'}
    $fixture=Join-Path $ProjectRoot 'target/gemini startup fixtures';New-Item -ItemType Directory -Path $fixture -Force|Out-Null
    $fakeMaven=Join-Path $fixture 'mvn.cmd'
    function Get-FinBridgeToolchain {param($ProjectRoot);return @{Maven=$fakeMaven}}
    $listener=New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Loopback,0);$listener.Start();$port=$listener.LocalEndpoint.Port
    $occupied=Invoke-FinBridgeGemini $port 'gemini-3.5-flash-lite' $false 10 3 30 6>&1
    Check ($occupied[-1] -eq 1 -and ($occupied|Out-String) -like '*start-gemini.cmd*') 'Occupied port fails with Gemini-specific instruction'
    Check ($listener.Server.IsBound) 'Existing port owner preserved';$listener.Stop()
    Set-Content -LiteralPath $fakeMaven -Value "@echo off`r`nexit /b 7" -Encoding ASCII
    $failed=Invoke-FinBridgeGemini $port 'gemini-3.5-flash-lite' $false 10 3 30 6>&1
    Check ($failed[-1] -eq 1 -and ($failed|Out-String) -like '*before readiness*') 'Maven early exit handled'
    $script:Ready=$false
    function Invoke-WebRequest {
        param([switch]$UseBasicParsing,$Uri,$TimeoutSec,$ErrorAction)
        if($Uri -notlike 'http://localhost:*/'){throw 'Unexpected endpoint; readiness must be GET home only'}
        return @{StatusCode=200;Content=$(if($script:Ready){'<aside data-testid="assistant-panel">READY</aside>'}else{'not ready'})}
    }
    $script:Start=${function:Start-FinBridgeGeminiProcess}
    function Start-FinBridgeGeminiProcess {
        param($ProjectRoot,$Maven,$Port,$Model,$ConnectTimeoutSeconds,$RequestTimeoutSeconds)
        $owned=& $script:Start @PSBoundParameters;$script:OwnedPid=$owned.Process.Id;return $owned
    }
    Set-Content -LiteralPath $fakeMaven -Value "@echo off`r`nping 127.0.0.1 -n 30 >nul" -Encoding ASCII
    $timed=Invoke-FinBridgeGemini $port 'gemini-3.5-flash-lite' $false 10 3 30 6>&1
    Check ($timed[-1] -eq 1 -and ($timed|Out-String) -like '*not ready within*') 'Readiness timeout handled'
    Check (-not(Get-Process -Id $script:OwnedPid -ErrorAction SilentlyContinue)) 'Owned process tree cleaned on timeout'
    $script:Ready=$true;$script:BrowserCalls=0
    function Start-Process {param($FilePath);$script:BrowserCalls++}
    Set-Content -LiteralPath $fakeMaven -Value "@echo off`r`nping 127.0.0.1 -n 3 >nul`r`nexit /b 0" -Encoding ASCII
    $ok=Invoke-FinBridgeGemini $port 'gemini-3.5-flash-lite' $true 10 3 30 6>&1
    Check ($ok[-1] -eq 0 -and ($ok|Out-String) -like '*startup sends zero model requests*') 'Supervised startup sends no model requests and does not claim AI readiness'
    Check ($script:BrowserCalls -eq 1) 'Browser opens only with explicit switch'
    Check (($ok|Out-String) -notlike '*synthetic-launcher-key*') 'Key never appears in launcher output'
    Check ((Get-Location).Path -eq $cwd -and $env:FINBRIDGE_LLM_PROVIDER -eq 'ollama' -and $env:FINBRIDGE_LLM_MODEL -eq 'qwen3:4b') 'Caller environment/cwd preserved'
} finally {$env:GEMINI_API_KEY=$priorKey;$env:FINBRIDGE_LLM_PROVIDER=$priorProvider;$env:FINBRIDGE_LLM_MODEL=$priorModel}
Write-Output "GEMINI_SCRIPT_CHECKS=$script:Checks PASS (stub processes; no live Gemini request)"
