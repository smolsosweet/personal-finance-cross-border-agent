param(
    [string]$Model = 'qwen3:4b',
    [string]$BaseUrl = 'http://localhost:11434',
    [int]$TimeoutSeconds = 60,
    [string]$FinBridgeUrl
)

function Assert-FinBridgeOllama {
    param([string]$Model, [string]$BaseUrl)
    try {
        $tags = Invoke-RestMethod -Uri ($BaseUrl.TrimEnd('/') + '/api/tags') -Method Get -TimeoutSec 5 -ErrorAction Stop
    } catch {
        throw "Ollama is unavailable at $BaseUrl. Start the Ollama desktop app, or run 'ollama serve' in another terminal. Keep it on localhost."
    }
    if (-not ($tags.models | Where-Object { $_.name -eq $Model })) {
        throw "Model $Model is not installed. Run: ollama pull $Model"
    }
}

function Invoke-FinBridgePreparation {
    param([string]$Model, [string]$BaseUrl, [int]$TimeoutSeconds, [string]$FinBridgeUrl)
    $ProgressPreference='SilentlyContinue'
    Assert-FinBridgeOllama -Model $Model -BaseUrl $BaseUrl
    $timer = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        if ($FinBridgeUrl) {
            $uri = [uri]$FinBridgeUrl
            if (-not $uri.IsLoopback -or $uri.Scheme -ne 'http') {
                throw 'Application warmup requires a local HTTP FinBridge URL.'
            }
            $url = $FinBridgeUrl.TrimEnd('/')
            # Fresh private session. This supported request is read-only and never resets data.
            $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
            Invoke-WebRequest -UseBasicParsing -Uri "$url/" -WebSession $session -TimeoutSec 5 -ErrorAction Stop | Out-Null
            $page = Invoke-WebRequest -UseBasicParsing -Uri "$url/agent/message" -Method Post -WebSession $session `
                -Body @{message='Show my configured budgets for this month.';language='en'} `
                -TimeoutSec ($TimeoutSeconds + 5) -ErrorAction Stop
            # This template is emitted only after a valid, high-confidence read-only classification.
            # A fresh session prevents a historical successful answer from hiding provider fallback.
            if ($page.Content -notmatch 'data-testid="assistant-replies"[\s\S]*Category budgets: VND\.') {
                throw 'FinBridge warmup did not return the validated budget result. AI fallback/clarification may be active; inspect the assistant and Audit Log.'
            }
            Write-Output ("FinBridge live intent warmup PASS in {0:N3} seconds (read-only budget; no reset or payment)." -f $timer.Elapsed.TotalSeconds)
        } else {
            # Preserve standalone model-weight preparation for existing callers.
            $body = @{model=$Model;messages=@(@{role='user';content='Reply READY only.'});stream=$false;think=$false;keep_alive='15m';options=@{temperature=0;num_predict=16}} | ConvertTo-Json -Depth 5
            $reply = Invoke-RestMethod -Uri ($BaseUrl.TrimEnd('/') + '/api/chat') -Method Post -ContentType 'application/json' -Body $body -TimeoutSec $TimeoutSeconds -ErrorAction Stop
            if (-not $reply.done -or [string]::IsNullOrWhiteSpace($reply.message.content)) {
                throw 'Ollama weight warmup did not complete. Keep timeout/fallback and investigate.'
            }
            Write-Output ("Ollama weight warmup completed in {0:N3} seconds; model retained for 15 minutes." -f $timer.Elapsed.TotalSeconds)
            Write-Output 'This warms weights only. To check the actual classifier on a running app, use -FinBridgeUrl http://localhost:8080.'
        }
    } finally { $timer.Stop() }
}

# Dot-sourcing exposes the shared preflight without warming or contacting the application.
if ($MyInvocation.InvocationName -ne '.') {
    $ErrorActionPreference = 'Stop'
    Invoke-FinBridgePreparation -Model $Model -BaseUrl $BaseUrl -TimeoutSeconds $TimeoutSeconds -FinBridgeUrl $FinBridgeUrl
}
