param(
    [ValidateRange(1,65535)][int]$Port = 8080,
    [ValidatePattern('^gemini-[A-Za-z0-9._-]+$')][string]$Model = 'gemini-3.5-flash-lite',
    [switch]$OpenBrowser,
    [ValidateRange(10,600)][int]$StartupTimeoutSeconds = 120,
    [ValidateRange(1,120)][int]$ConnectTimeoutSeconds = 3,
    [ValidateRange(1,120)][int]$RequestTimeoutSeconds = 30
)

# Shared process helpers; no model provider is started on this machine.
. (Join-Path $PSScriptRoot 'startup-common.ps1')

function New-FinBridgeGeminiProcessInfo {
    param([string]$ProjectRoot,[string]$Maven,[int]$Port,[string]$Model,[int]$ConnectTimeoutSeconds,[int]$RequestTimeoutSeconds)
    $info=New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName=Join-Path $env:SystemRoot 'System32\cmd.exe'
    $info.WorkingDirectory=$ProjectRoot
    $info.UseShellExecute=$false;$info.CreateNoWindow=$true
    $info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
    # Secret remains inherited in the child environment; it is never a CLI argument.
    $appArgs='--spring.profiles.active=gemini --server.port='+$Port+' --finbridge.llm.enabled=true --finbridge.llm.model='+$Model+' --finbridge.llm.connect-timeout='+$ConnectTimeoutSeconds+'s --finbridge.llm.request-timeout='+$RequestTimeoutSeconds+'s'
    $info.Arguments='/d /s /c ""'+$Maven+'" -B spring-boot:run "-Dspring-boot.run.arguments='+$appArgs+'" "-Dspring-boot.run.jvmArguments=-Dspring.main.add-command-line-properties=true""'
    foreach ($setting in @{FINBRIDGE_LLM_ENABLED='true';FINBRIDGE_LLM_MODEL=$Model;FINBRIDGE_LLM_CONNECT_TIMEOUT=($ConnectTimeoutSeconds.ToString()+'s');FINBRIDGE_LLM_REQUEST_TIMEOUT=($RequestTimeoutSeconds.ToString()+'s');SPRING_PROFILES_ACTIVE='gemini';SPRING_MAIN_ADD_COMMAND_LINE_PROPERTIES='true'}.GetEnumerator()) {
        $info.EnvironmentVariables[$setting.Key]=$setting.Value
    }
    $info.EnvironmentVariables.Remove('SPRING_APPLICATION_JSON')
    $info.EnvironmentVariables.Remove('SPRING_PROFILES_INCLUDE')
    return $info
}

function Start-FinBridgeGeminiProcess {
    param([string]$ProjectRoot,[string]$Maven,[int]$Port,[string]$Model,[int]$ConnectTimeoutSeconds,[int]$RequestTimeoutSeconds)
    $process=New-Object System.Diagnostics.Process
    $process.StartInfo=New-FinBridgeGeminiProcessInfo @PSBoundParameters
    if (-not $process.Start()) { throw 'Could not launch Maven.' }
    return @{Process=$process;Out=$process.StandardOutput.ReadLineAsync();Err=$process.StandardError.ReadLineAsync()}
}

function Invoke-FinBridgeGemini {
    param([int]$Port,[string]$Model,[bool]$OpenBrowser,[int]$StartupTimeoutSeconds,[int]$ConnectTimeoutSeconds,[int]$RequestTimeoutSeconds)
    $ErrorActionPreference='Stop';$ProgressPreference='SilentlyContinue'
    $root=Split-Path -Parent $PSScriptRoot
    $run=$null
    try {
        if ([string]::IsNullOrWhiteSpace($env:GEMINI_API_KEY)) {
            throw 'GEMINI_API_KEY is missing in this terminal. Run the launcher in the terminal where the key is configured. Never paste the key into chat.'
        }
        if ($Model -notmatch '^gemini-[A-Za-z0-9._-]+$') { throw 'Specify an explicit valid Gemini model using -Model.' }
        $tools=Get-FinBridgeToolchain -ProjectRoot $root
        # Keep occupied-process handling separate: never kill an existing application's owner.
        try { Assert-FinBridgePort -Port $Port }
        catch { throw "Port $Port is already occupied. Stop its owner or use scripts\start-gemini.cmd -Port $($Port+1). No process was killed." }
        Write-Host "Starting FinBridge (Gemini / $Model) on port $Port."
        $run=Start-FinBridgeGeminiProcess -ProjectRoot $root -Maven $tools.Maven -Port $Port -Model $Model -ConnectTimeoutSeconds $ConnectTimeoutSeconds -RequestTimeoutSeconds $RequestTimeoutSeconds
        $url="http://localhost:$Port"
        $deadline=[DateTime]::UtcNow.AddSeconds($StartupTimeoutSeconds);$ready=$false
        while ([DateTime]::UtcNow -lt $deadline) {
            Write-FinBridgeProcessLogs -Run $run
            if ($run.Process.HasExited) { Write-FinBridgeProcessLogs -Run $run;throw "Application exited before readiness (exit $($run.Process.ExitCode)). Check the logs above." }
            try {
                $page=Invoke-WebRequest -UseBasicParsing -Uri "$url/" -TimeoutSec 2 -ErrorAction Stop
                if ($page.StatusCode -eq 200 -and $page.Content -match 'data-testid="assistant-panel"') { $ready=$true;break }
            } catch { }
            Start-Sleep -Milliseconds 250
        }
        if (-not $ready) { throw "Application was not ready within $StartupTimeoutSeconds seconds." }
        Write-Host "Application ready: $url`nProfile: gemini | Provider: gemini | Model: $Model`nAI readiness: NOT VERIFIED (startup sends zero model requests).`nStop: Ctrl+C in this terminal."
        if ($OpenBrowser) {
            try { Start-Process -FilePath $url | Out-Null }
            catch { Write-Warning "Open $url manually; application remains running." }
        }
        while (-not $run.Process.HasExited) { Write-FinBridgeProcessLogs -Run $run;Start-Sleep -Milliseconds 200 }
        Write-FinBridgeProcessLogs -Run $run
        return $run.Process.ExitCode
    } catch [System.Management.Automation.PipelineStoppedException] {
        return 130
    } catch {
        Write-Host ('FinBridge Gemini startup failed: '+$_.Exception.Message) -ForegroundColor Red
        return 1
    } finally {
        Stop-FinBridgeOwnedProcess -Run $run
        if ($null -ne $run) { $run.Process.Dispose() }
        # Overrides/working directory belong to the child. Caller environment and cwd never change.
    }
}

if ($MyInvocation.InvocationName -ne '.') {
    exit (Invoke-FinBridgeGemini -Port $Port -Model $Model -OpenBrowser $OpenBrowser.IsPresent -StartupTimeoutSeconds $StartupTimeoutSeconds -ConnectTimeoutSeconds $ConnectTimeoutSeconds -RequestTimeoutSeconds $RequestTimeoutSeconds)
}
