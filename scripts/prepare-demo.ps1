param(
    [string]$Model = "qwen3:4b",
    [string]$BaseUrl = "http://localhost:11434",
    [int]$TimeoutSeconds = 60
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd('/')
$tags = Invoke-RestMethod -Uri "$BaseUrl/api/tags" -Method Get -TimeoutSec 5
if (-not ($tags.models | Where-Object { $_.name -eq $Model })) {
    throw "Model $Model is not installed. Run 'ollama list' and install it before the demo."
}

# Synthetic warm-up only. This script never calls FinBridge or creates a payment plan.
$body = @{
    model = $Model
    messages = @(@{ role = "user"; content = "Reply READY only." })
    stream = $false
    think = $false
    keep_alive = "15m"
    options = @{ temperature = 0; num_predict = 16 }
} | ConvertTo-Json -Depth 5

$timer = [System.Diagnostics.Stopwatch]::StartNew()
try {
    $reply = Invoke-RestMethod -Uri "$BaseUrl/api/chat" -Method Post -ContentType "application/json" -Body $body -TimeoutSec $TimeoutSeconds
} finally {
    $timer.Stop()
}
if (-not $reply.done -or [string]::IsNullOrWhiteSpace($reply.message.content)) {
    throw "Ollama warm-up did not complete. Keep the app timeout/fallback and investigate before presenting."
}
Write-Output ("Ollama warm-up completed in {0:N3} seconds; model retained for 15 minutes." -f $timer.Elapsed.TotalSeconds)
Write-Output "Before presenting, also ask one read-only budget question in FinBridge, measure the wait, then reset synthetic demo data if needed."
Write-Output "Warm-up success is not an application or payment verification result. No automatic retry was performed."
