param(
    [ValidateRange(1,65535)][int]$Port = 8080,
    [switch]$SkipWarmup,
    [switch]$OpenBrowser,
    [ValidateRange(10,600)][int]$StartupTimeoutSeconds = 120
)

function Get-FinBridgeToolchain {
    param([string]$ProjectRoot)
    if ($env:JAVA_HOME) {
        $java = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (-not (Test-Path -LiteralPath $java)) { throw 'JAVA_HOME is invalid. Point it at an installed JDK 21 (not its bin folder), then open a new terminal.' }
    } else {
        $command = Get-Command java -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $command) { throw 'Java is missing. Install JDK 21 and set JAVA_HOME/PATH, then open a new PowerShell terminal.' }
        $java = $command.Source
    }
    # Java writes its version to stderr; PS 5.1 must not treat that as a terminating error.
    $priorPreference = $ErrorActionPreference
    try { $ErrorActionPreference='Continue'; $version=(& $java -version 2>&1 | Out-String); $javaCode=$LASTEXITCODE }
    finally { $ErrorActionPreference=$priorPreference }
    if ($javaCode -ne 0 -or $version -notmatch 'version "([0-9]+)') { throw 'Unable to check Java. Verify java -version and JAVA_HOME.' }
    if ([int]$Matches[1] -lt 21) { throw 'FinBridge requires JDK 21 or newer. Update JAVA_HOME/PATH.' }
    $wrapper = Join-Path $ProjectRoot 'mvnw.cmd'
    if (Test-Path -LiteralPath $wrapper) { $maven=$wrapper }
    else {
        $command=Get-Command mvn.cmd -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $command) { throw 'Maven is missing and this checkout has no mvnw.cmd. Install Maven 3.9+ and add its bin folder to PATH, then open a new terminal.' }
        $maven=$command.Source
    }
    $mavenVersion=(& $maven -version | Out-String)
    if ($LASTEXITCODE -ne 0 -or $mavenVersion -notmatch 'Apache Maven (\d+)\.(\d+)') { throw 'Maven could not start. Run mvn -version and check JAVA_HOME/PATH.' }
    if ([int]$Matches[1] -lt 3 -or ([int]$Matches[1] -eq 3 -and [int]$Matches[2] -lt 9)) { throw 'Use Maven 3.9 or newer.' }
    return @{Java=$java;Maven=$maven}
}

function Assert-FinBridgePort {
    param([int]$Port)
    $listeners=[System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners()
    if ($listeners | Where-Object { $_.Port -eq $Port }) {
        throw "Port $Port is already occupied. Stop its owner yourself or choose: .\scripts\start-local.cmd -Port $($Port+1). No process was killed."
    }
}

function Start-FinBridgeOcrService {
    param([string]$ProjectRoot,[int]$TimeoutSeconds=120)
    $healthUrl='http://127.0.0.1:8099/health'
    # Reuse a healthy OCR service already listening on the configured loopback port.
    try {
        $health=Invoke-RestMethod -Uri $healthUrl -TimeoutSec 2 -ErrorAction Stop
        if ($health.status -eq 'ok' -and $health.engine -eq 'PaddleOCR') {
            Write-Host 'Bill reading: reusing the running local PaddleOCR service.'
            return $null
        }
        throw 'Port 8099 is occupied by a service that is not FinBridge PaddleOCR.'
    } catch {
        if ($_.Exception.Message -like '*occupied by a service*') { throw }
        # Invoke-RestMethod wraps connection-refused differently across PowerShell versions.
    }
    $python=Join-Path $ProjectRoot '.venv-bill-ai\Scripts\python.exe'
    $server=Join-Path $ProjectRoot 'tools\bill-ai-local\server.py'
    if (-not (Test-Path -LiteralPath $python)) { throw 'The local bill AI environment is missing. Create/fix .venv-bill-ai first, then run .\.venv-bill-ai\Scripts\python.exe -m pip install -r tools\bill-ai-local\requirements.txt.' }
    if (-not (Test-Path -LiteralPath $server)) { throw 'The local PaddleOCR server script is missing from tools\bill-ai-local.' }
    $logBase=Join-Path ([System.IO.Path]::GetTempPath()) 'finbridge-bill-ocr'
    $process=Start-Process -FilePath $python -ArgumentList @($server) -WorkingDirectory $ProjectRoot -WindowStyle Hidden -RedirectStandardOutput "$logBase.stdout.log" -RedirectStandardError "$logBase.stderr.log" -PassThru
    Write-Host 'Starting the local PaddleOCR service from .venv-bill-ai...'
    $deadline=[DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    try {
        while ([DateTime]::UtcNow -lt $deadline) {
            if ($process.HasExited) {
                $tail=if (Test-Path -LiteralPath "$logBase.stderr.log") { (Get-Content -LiteralPath "$logBase.stderr.log" -Tail 12 | Out-String).Trim() } else { '' }
                throw "PaddleOCR exited during startup (exit $($process.ExitCode)). $tail"
            }
            try {
                $health=Invoke-RestMethod -Uri $healthUrl -TimeoutSec 2 -ErrorAction Stop
                if ($health.status -eq 'ok' -and $health.engine -eq 'PaddleOCR') {
                    Write-Host 'Bill reading: local PaddleOCR is ready on http://127.0.0.1:8099.'
                    return $process
                }
            } catch { }
            Start-Sleep -Milliseconds 500
        }
        throw "PaddleOCR did not become ready within $TimeoutSeconds seconds. Check $logBase.stderr.log."
    } catch {
        Stop-FinBridgeOcrService -Process $process
        throw
    }
}

function Stop-FinBridgeOcrService {
    param([System.Diagnostics.Process]$Process)
    if ($null -ne $Process -and -not $Process.HasExited) {
        Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
        [void]$Process.WaitForExit(5000)
    }
}

function Start-FinBridgeOwnedProcess {
    param([string]$ProjectRoot,[string]$Maven,[int]$Port,[string]$DocumentApiKey)
    $info=New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName=Join-Path $env:SystemRoot 'System32\cmd.exe'
    $info.WorkingDirectory=$ProjectRoot
    $info.UseShellExecute=$false
    $info.CreateNoWindow=$true
    $info.RedirectStandardOutput=$true
    $info.RedirectStandardError=$true
    # CLI properties take precedence over profile/env/JSON. Child environment is never persisted.
    $applicationArguments='--spring.profiles.active=local --server.port='+$Port+' --finbridge.llm.enabled=true --finbridge.llm.provider=ollama --finbridge.llm.model=qwen3:4b --finbridge.llm.base-url=http://localhost:11434 --finbridge.llm.connect-timeout=3s --finbridge.llm.request-timeout=60s'
    $info.Arguments='/d /s /c ""'+$Maven+'" -B spring-boot:run "-Dspring-boot.run.arguments='+$applicationArguments+'" "-Dspring-boot.run.jvmArguments=-Dspring.main.add-command-line-properties=true""'
    foreach ($setting in @{FINBRIDGE_LLM_ENABLED='true';FINBRIDGE_LLM_PROVIDER='ollama';FINBRIDGE_LLM_MODEL='qwen3:4b';FINBRIDGE_LLM_BASE_URL='http://localhost:11434';FINBRIDGE_LLM_CONNECT_TIMEOUT='3s';FINBRIDGE_LLM_REQUEST_TIMEOUT='60s';SPRING_PROFILES_ACTIVE='local';SPRING_MAIN_ADD_COMMAND_LINE_PROPERTIES='true'}.GetEnumerator()) {
        $info.EnvironmentVariables[$setting.Key]=$setting.Value
    }
    if (-not [string]::IsNullOrWhiteSpace($DocumentApiKey)) {
        # OCR key is passed only through the child environment, never CLI arguments or a file.
        $info.EnvironmentVariables['GEMINI_API_KEY']=$DocumentApiKey
    }
    $info.EnvironmentVariables.Remove('SPRING_APPLICATION_JSON')
    $info.EnvironmentVariables.Remove('SPRING_PROFILES_INCLUDE')
    $process=New-Object System.Diagnostics.Process
    $process.StartInfo=$info
    if (-not $process.Start()) { throw 'Could not launch Maven.' }
    return @{Process=$process;Out=$process.StandardOutput.ReadLineAsync();Err=$process.StandardError.ReadLineAsync()}
}

function Write-FinBridgeProcessLogs {
    param([hashtable]$Run)
    foreach ($key in @('Out','Err')) {
        while ($null -ne $Run[$key] -and $Run[$key].IsCompleted) {
            $line=$Run[$key].GetAwaiter().GetResult()
            if ($null -eq $line) { $Run[$key]=$null; break }
            Write-Host $line
            if ($key -eq 'Out') { $Run[$key]=$Run.Process.StandardOutput.ReadLineAsync() }
            else { $Run[$key]=$Run.Process.StandardError.ReadLineAsync() }
        }
    }
}

function Stop-FinBridgeOwnedProcess {
    param([hashtable]$Run)
    if ($null -eq $Run -or $Run.Process.HasExited) { return }
    # .NET calls also work in finally when PowerShell's pipeline is interrupted by Ctrl+C.
    # /T is rooted at the process created above; it never targets Ollama or other Java processes.
    $info=New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName=Join-Path $env:SystemRoot 'System32\taskkill.exe'
    $info.Arguments='/PID '+$Run.Process.Id+' /T /F'
    $info.UseShellExecute=$false;$info.CreateNoWindow=$true
    $info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
    $killer=[System.Diagnostics.Process]::Start($info)
    [void]$killer.WaitForExit(5000)
    [void]$Run.Process.WaitForExit(5000)
    $killer.Dispose()
}

function Invoke-FinBridgeLocal {
    param([int]$Port,[bool]$SkipWarmup,[bool]$OpenBrowser,[int]$StartupTimeoutSeconds)
    $ErrorActionPreference='Stop'
    $ProgressPreference='SilentlyContinue'
    $root=Split-Path -Parent $PSScriptRoot
    $run=$null
    $ocrProcess=$null
    try {
        $tools=Get-FinBridgeToolchain -ProjectRoot $root
        Assert-FinBridgePort -Port $Port
        $ocrProcess=Start-FinBridgeOcrService -ProjectRoot $root -TimeoutSeconds $StartupTimeoutSeconds
        . (Join-Path $PSScriptRoot 'prepare-demo.ps1')
        Assert-FinBridgeOllama -Model 'qwen3:4b' -BaseUrl 'http://localhost:11434'
        $documentApiKey=$env:GEMINI_API_KEY
        $secureDocumentApiKey=$null
        $keyConfigPath=Join-Path $env:LOCALAPPDATA 'FinBridge\gemini-api-key.dpapi'
        if ([string]::IsNullOrWhiteSpace($documentApiKey) -and (Test-Path -LiteralPath $keyConfigPath)) {
            try {
                $encryptedKey=[System.IO.File]::ReadAllText($keyConfigPath).Trim()
                if ($encryptedKey) {
                    $secureDocumentApiKey=ConvertTo-SecureString -String $encryptedKey
                    $documentApiKey=[System.Net.NetworkCredential]::new('', $secureDocumentApiKey).Password
                    Write-Host 'Bill reading: loaded your Windows-encrypted local Gemini key.'
                }
            } catch {
                $secureDocumentApiKey=$null
                $documentApiKey=$null
                Write-Warning 'Could not decrypt the saved Gemini key for this Windows account. Enter it again to replace the local key.'
            }
        }
        if ([string]::IsNullOrWhiteSpace($documentApiKey)) {
            $secureDocumentApiKey=Read-Host 'Gemini API key for bill reading (input hidden; saved encrypted for this Windows account)' -AsSecureString
            if ($secureDocumentApiKey.Length -gt 0) {
                $documentApiKey=[System.Net.NetworkCredential]::new('', $secureDocumentApiKey).Password
                $keyDirectory=Split-Path -Parent $keyConfigPath
                if (-not (Test-Path -LiteralPath $keyDirectory)) {
                    New-Item -ItemType Directory -Path $keyDirectory -Force | Out-Null
                }
                $encryptedKey=ConvertFrom-SecureString -SecureString $secureDocumentApiKey
                [System.IO.File]::WriteAllText($keyConfigPath,$encryptedKey,[System.Text.Encoding]::ASCII)
                Write-Host 'Gemini key saved encrypted in your Windows user profile for future launches.'
            }
        }
        if ([string]::IsNullOrWhiteSpace($documentApiKey)) {
            Write-Host 'Bill reading: disabled (no Gemini key supplied). The rest of FinBridge can still start.'
        } else {
            Write-Host 'Bill reading: Gemini key configured for the app process.'
        }
        Write-Host "Starting FinBridge (local / ollama / qwen3:4b) on port $Port. Ctrl+C stops only this application's process tree."
        $run=Start-FinBridgeOwnedProcess -ProjectRoot $root -Maven $tools.Maven -Port $Port -DocumentApiKey $documentApiKey
        $url="http://localhost:$Port"
        $deadline=[DateTime]::UtcNow.AddSeconds($StartupTimeoutSeconds)
        $ready=$false
        while ([DateTime]::UtcNow -lt $deadline) {
            Write-FinBridgeProcessLogs -Run $run
            if ($run.Process.HasExited) { Write-FinBridgeProcessLogs -Run $run; throw "Maven/application exited before readiness (exit $($run.Process.ExitCode)). Check the logs above." }
            try {
                $page=Invoke-WebRequest -UseBasicParsing -Uri "$url/" -TimeoutSec 2 -ErrorAction Stop
                if ($page.StatusCode -eq 200 -and $page.Content -match 'data-testid="assistant-panel"') { $ready=$true;break }
            } catch { }
            Start-Sleep -Milliseconds 250
        }
        if (-not $ready) { throw "Startup did not become ready within $StartupTimeoutSeconds seconds. Check the logs; only this launcher's process tree will be stopped." }
        Write-Host "Application ready: $url"
        $warmup='SKIPPED (AI readiness not verified)'
        if (-not $SkipWarmup) {
            Write-Host 'Warming the actual read-only FinBridge classifier; the first request may take up to 60 seconds...'
            try {
                & (Join-Path $PSScriptRoot 'prepare-demo.ps1') -FinBridgeUrl $url -Model 'qwen3:4b' -BaseUrl 'http://localhost:11434' -TimeoutSeconds 60 | ForEach-Object { Write-Host $_ }
                $warmup='PASS (live read-only budget classification)'
            } catch {
                $warmup='FAILED (application remains available; AI fallback may be active)'
                Write-Warning $_.Exception.Message
            }
        }
        Write-FinBridgeProcessLogs -Run $run
        if ($run.Process.HasExited) { throw "Application stopped during warmup (exit $($run.Process.ExitCode))." }
        Write-Host "URL: $url`nProfile: local | Provider: ollama | Model: qwen3:4b`nWarmup: $warmup`nStop: Ctrl+C in this terminal."
        if ($OpenBrowser) {
            try { Start-Process -FilePath $url | Out-Null }
            catch { Write-Warning "Could not open the browser. Open $url manually; the application remains running." }
        }
        while (-not $run.Process.HasExited) { Write-FinBridgeProcessLogs -Run $run;Start-Sleep -Milliseconds 200 }
        Write-FinBridgeProcessLogs -Run $run
        return $run.Process.ExitCode
    } catch [System.Management.Automation.PipelineStoppedException] {
        return 130
    } catch {
        Write-Host ('FinBridge startup failed: '+$_.Exception.Message) -ForegroundColor Red
        return 1
    } finally {
        Stop-FinBridgeOwnedProcess -Run $run
        Stop-FinBridgeOcrService -Process $ocrProcess
        if ($null -ne $run) { $run.Process.Dispose() }
        if ($null -ne $secureDocumentApiKey) { $secureDocumentApiKey.Dispose() }
        $documentApiKey=$null
        # Caller cwd/environment never changed: working directory and overrides belong to the child only.
    }
}

if ($MyInvocation.InvocationName -ne '.') {
    exit (Invoke-FinBridgeLocal -Port $Port -SkipWarmup $SkipWarmup.IsPresent -OpenBrowser $OpenBrowser.IsPresent -StartupTimeoutSeconds $StartupTimeoutSeconds)
}
