param([string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path)
$ErrorActionPreference='Stop'
. (Join-Path $ProjectRoot 'scripts/start-local.ps1')
. (Join-Path $ProjectRoot 'scripts/prepare-demo.ps1')
$script:Checks=0
function Check([bool]$Value,[string]$Name) {
    if (-not $Value) { throw "FAIL: $Name" }
    $script:Checks++;Write-Output "PASS: $Name"
}
function Reject([scriptblock]$Action,[string]$Expected,[string]$Name) {
    $message=''
    try { & $Action | Out-Null } catch { $message=$_.Exception.Message }
    Check ($message -like "*$Expected*") $Name
}
foreach ($file in @('scripts/start-local.ps1','scripts/prepare-demo.ps1')) {
    $parseErrors=$null
    [void][System.Management.Automation.Language.Parser]::ParseFile((Join-Path $ProjectRoot $file),[ref]$null,[ref]$parseErrors)
    Check ($parseErrors.Count -eq 0) "PS syntax: $file"
}
$toolchain=Get-FinBridgeToolchain -ProjectRoot $ProjectRoot
Check ($toolchain.Maven -and $toolchain.Java) 'Installed Java and Maven detected'
$fixture=Join-Path $ProjectRoot 'target/startup script fixtures'
New-Item -ItemType Directory -Path $fixture -Force | Out-Null
$wrapper=Join-Path $fixture 'mvnw.cmd'
Set-Content -LiteralPath $wrapper -Value '@echo Apache Maven 3.9.16' -Encoding ASCII
Check ((Get-FinBridgeToolchain -ProjectRoot $fixture).Maven -eq $wrapper) 'Existing Windows Maven wrapper preferred (path with spaces)'
$script:MissingMaven=$true
function Get-Command {
    param($Name,$CommandType,$ErrorAction)
    if ($script:MissingMaven -and $Name -eq 'mvn.cmd') { return $null }
    Microsoft.PowerShell.Core\Get-Command @PSBoundParameters
}
Reject { Get-FinBridgeToolchain -ProjectRoot $ProjectRoot } 'Maven is missing' 'Missing Maven actionable (stub)'
$script:MissingMaven=$false
$priorJava=$env:JAVA_HOME
try {
    $env:JAVA_HOME=Join-Path $fixture 'missing-jdk'
    Reject { Get-FinBridgeToolchain -ProjectRoot $ProjectRoot } 'JAVA_HOME is invalid' 'Missing JDK actionable'
} finally { $env:JAVA_HOME=$priorJava }
$listener=New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Loopback,0)
$listener.Start();$occupied=$listener.LocalEndpoint.Port
try {
    Reject { Assert-FinBridgePort -Port $occupied } 'already occupied' 'Occupied port rejected'
    $client=New-Object System.Net.Sockets.TcpClient
    $client.Connect('localhost',$occupied);Check $client.Connected 'Occupied port owner preserved';$client.Dispose()
} finally { $listener.Stop() }

# Bounded stubs simulate missing prerequisites/provider failure without uninstalling anything.
$script:OllamaMode='missing'
function Invoke-RestMethod {
    param($Uri,$Method,$TimeoutSec,$ErrorAction,$ContentType,$Body)
    if ($script:OllamaMode -eq 'offline') { throw 'Synthetic connection failure' }
    if ($script:OllamaMode -eq 'missing') { return @{models=@()} }
    if ($Uri -like '*/api/tags') { return @{models=@(@{name='qwen3:4b'})} }
    $script:WeightCalls++
    return @{done=$true;message=@{content='READY'}}
}
Reject { Assert-FinBridgeOllama 'qwen3:4b' 'http://localhost:11434' } 'ollama pull qwen3:4b' 'Missing model actionable'
$script:OllamaMode='offline'
Reject { Assert-FinBridgeOllama 'qwen3:4b' 'http://localhost:11434' } 'ollama serve' 'Unreachable Ollama actionable'
$script:OllamaMode='ready';$script:WeightCalls=0
Invoke-FinBridgePreparation 'qwen3:4b' 'http://localhost:11434' 60 '' | Out-Null
Check ($script:WeightCalls -eq 1) 'Legacy standalone weight warmup preserved (stub)'
$script:WarmMode='fallback';$script:ReadOnlyCalls=0
function Invoke-WebRequest {
    param([switch]$UseBasicParsing,$Uri,$WebSession,$TimeoutSec,$ErrorAction,$Method,$Body)
    if ($Method -eq 'Post') {
        if ($Uri -notlike '*/agent/message' -or $Body.message -ne 'Show my configured budgets for this month.') { throw 'Unexpected mutating warmup request' }
        $script:ReadOnlyCalls++
        if ($script:WarmMode -eq 'success') { return @{Content='<div data-testid="assistant-replies">Category budgets: VND.</div>'} }
        return @{Content='AI is temporarily unavailable'}
    }
    if ($script:AppReady) { return @{StatusCode=200;Content='<aside data-testid="assistant-panel">READY</aside>'} }
    return @{StatusCode=200;Content='not yet ready'}
}
Reject { Invoke-FinBridgePreparation 'qwen3:4b' 'http://localhost:11434' 60 'http://localhost:8112' } 'fallback/clarification' 'Fallback never reported as live readiness (stub)'
$script:WarmMode='success'
Invoke-FinBridgePreparation 'qwen3:4b' 'http://localhost:11434' 60 'http://localhost:8112' | Out-Null
Check ($script:ReadOnlyCalls -eq 2 -and $script:WeightCalls -eq 1) 'App warmup uses only actual read-only endpoint, no duplicate weight call (stub)'
Reject { Invoke-FinBridgePreparation 'qwen3:4b' 'http://localhost:11434' 60 'http://example.com' } 'local HTTP' 'Warmup restricted to localhost'

$failure=Join-Path $fixture 'fail-maven.cmd'
Set-Content -LiteralPath $failure -Value "@echo off`r`nexit /b 7" -Encoding ASCII
function Get-FinBridgeToolchain { param($ProjectRoot);return @{Java=$toolchain.Java;Maven=$failure} }
$probeListener=New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Loopback,0)
$probeListener.Start();$freePort=$probeListener.LocalEndpoint.Port;$probeListener.Stop()
$cwd=(Get-Location).Path;$provider=$env:FINBRIDGE_LLM_PROVIDER
Check ((Invoke-FinBridgeLocal $freePort $true $false 10) -eq 1) 'Process exits before readiness: nonzero result'
Check ((Get-Location).Path -eq $cwd -and $env:FINBRIDGE_LLM_PROVIDER -eq $provider) 'Caller cwd/environment preserved on failure'
$script:OriginalStart=${function:Start-FinBridgeOwnedProcess}
function Start-FinBridgeOwnedProcess {
    param($ProjectRoot,$Maven,$Port)
    $owned=& $script:OriginalStart $ProjectRoot $Maven $Port
    $script:LastOwnedPid=$owned.Process.Id
    return $owned
}
Set-Content -LiteralPath $failure -Value "@echo off`r`nping 127.0.0.1 -n 30 >nul`r`nexit /b 0" -Encoding ASCII
Check ((Invoke-FinBridgeLocal $freePort $true $false 10) -eq 1) 'Readiness timeout produces nonzero result (stub application)'
Check (-not (Get-Process -Id $script:LastOwnedPid -ErrorAction SilentlyContinue)) 'Timeout cleans only launcher-owned process tree'
$script:AppReady=$true;$script:WarmMode='fallback';$script:BrowserCalls=0
function Start-Process { param($FilePath);$script:BrowserCalls++;return $null }
Set-Content -LiteralPath $failure -Value "@echo off`r`nping 127.0.0.1 -n 4 >nul`r`nexit /b 0" -Encoding ASCII
$clock=[Diagnostics.Stopwatch]::StartNew()
$warmResult=Invoke-FinBridgeLocal $freePort $false $true 10 6>&1 3>&1
Check ($warmResult[-1] -eq 0 -and $clock.Elapsed.TotalSeconds -ge 2) 'Warmup fallback keeps supervised application alive until its own exit (stub)'
Check (($warmResult | Out-String) -like '*Warmup: FAILED*') 'Warmup failure clearly labelled, not AI-ready (stub)'
Check ($script:BrowserCalls -eq 1) 'Browser opened only with explicit switch (stub)'
$calls=$script:ReadOnlyCalls
$skipResult=Invoke-FinBridgeLocal $freePort $true $false 10 6>&1 3>&1
Check ($skipResult[-1] -eq 0 -and $script:ReadOnlyCalls -eq $calls) 'SkipWarmup sends no model/application warmup (stub)'
Check ($script:BrowserCalls -eq 1 -and ($skipResult | Out-String) -like '*Warmup: SKIPPED*') 'Default does not open browser and does not claim live AI readiness (stub)'
Write-Output "SCRIPT_CHECKS=$script:Checks PASS (prerequisite/provider errors use explicit stubs; not real model evidence)"
