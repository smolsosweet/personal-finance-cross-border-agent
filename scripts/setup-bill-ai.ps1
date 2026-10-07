[CmdletBinding()]
param(
    [switch]$SkipOllamaModel
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$projectRoot = Split-Path -Parent $PSScriptRoot
$requirements = Join-Path $projectRoot 'tools\bill-ai-local\requirements.txt'
$venvPython = Join-Path $projectRoot '.venv-bill-ai\Scripts\python.exe'

function Stop-Setup([string]$Message) {
    Write-Error $Message
    exit 1
}

if (-not (Test-Path -LiteralPath $requirements)) {
    Stop-Setup 'Missing tools\bill-ai-local\requirements.txt. Run this script from a complete FinBridge checkout.'
}

# Prefer the Python launcher so a system-wide Python 3.12 can be selected explicitly.
$pythonCommand = Get-Command py -ErrorAction SilentlyContinue
$basePython = $null
if ($null -ne $pythonCommand) {
    & $pythonCommand.Source -3.12 -c "import sys,struct; print(sys.executable); print(f'{sys.version_info.major}.{sys.version_info.minor}.{struct.calcsize('P')*8}')"
    if ($LASTEXITCODE -eq 0) { $basePython = (& $pythonCommand.Source -3.12 -c "import sys; print(sys.executable)").Trim() }
}
if ([string]::IsNullOrWhiteSpace($basePython)) {
    $pythonCommand = Get-Command python -ErrorAction SilentlyContinue
    if ($null -ne $pythonCommand) {
        $version = & $pythonCommand.Source -c "import sys,struct; print(f'{sys.version_info.major}.{sys.version_info.minor}.{struct.calcsize('P')*8}')" 2>$null
        if ($LASTEXITCODE -eq 0 -and $version.Trim() -eq '3.12.64') { $basePython = $pythonCommand.Source }
    }
}
if ([string]::IsNullOrWhiteSpace($basePython)) {
    Stop-Setup 'Python 3.12 64-bit is required for PaddleOCR on Windows. Install Python 3.12 x64, enable the Python Launcher or add python.exe to PATH, then rerun this script.'
}

Write-Host "Using Python: $basePython"
if (-not (Test-Path -LiteralPath $venvPython)) {
    Write-Host 'Creating the project-local .venv-bill-ai environment...'
    & $basePython -m venv (Join-Path $projectRoot '.venv-bill-ai')
    if ($LASTEXITCODE -ne 0) { Stop-Setup 'Could not create .venv-bill-ai.' }
}

Write-Host 'Installing PaddleOCR and the local bill AI service dependencies...'
& $venvPython -m pip install --upgrade pip
if ($LASTEXITCODE -ne 0) { Stop-Setup 'Could not upgrade pip in .venv-bill-ai.' }
& $venvPython -m pip install -r $requirements
if ($LASTEXITCODE -ne 0) { Stop-Setup 'Dependency installation failed. Check network access and rerun this script.' }

Write-Host 'Verifying PaddleOCR imports...'
& $venvPython -c "import pydantic, paddle; from paddleocr import PaddleOCR; print('PaddleOCR OK'); print('pydantic=' + pydantic.__version__); print('paddlepaddle=' + paddle.__version__)"
if ($LASTEXITCODE -ne 0) { Stop-Setup 'PaddleOCR verification failed. Review the Python package error above.' }

if (-not $SkipOllamaModel) {
    $ollamaCommand = Get-Command ollama -ErrorAction SilentlyContinue
    if ($null -eq $ollamaCommand) {
        Write-Warning 'Ollama was not found. Local Qwen extraction also needs Ollama and qwen3:4b; install Ollama, open it, then run: ollama pull qwen3:4b'
    } else {
        $tags = $null
        try { $tags = Invoke-RestMethod -Uri 'http://localhost:11434/api/tags' -TimeoutSec 3 } catch { }
        if ($null -eq $tags) {
            Write-Warning 'Ollama is installed but its local server is not responding. Open Ollama, then run: ollama pull qwen3:4b'
        } else {
            $hasModel = @($tags.models | Where-Object { $_.name -eq 'qwen3:4b' -or $_.name -like 'qwen3:4b:*' }).Count -gt 0
            if (-not $hasModel) {
                Write-Host 'Downloading the required Ollama model qwen3:4b (this can take a while)...'
                & $ollamaCommand.Source pull qwen3:4b
                if ($LASTEXITCODE -ne 0) { Stop-Setup 'Ollama could not download qwen3:4b. Start Ollama and run this script again.' }
            } else {
                Write-Host 'Ollama model qwen3:4b is already installed.'
            }
        }
    }
}

Write-Host ''
Write-Host 'Local bill AI setup is complete.' -ForegroundColor Green
Write-Host 'Start FinBridge with: .\scripts\start-local.ps1 -Port 8080'
Write-Host 'The first local OCR run may download PaddleOCR model weights.'
