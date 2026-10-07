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
        throw "Port $Port is already occupied. Stop its owner yourself or choose: scripts\start-gemini.cmd -Port $($Port+1). No process was killed."
    }
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
