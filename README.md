# FinBridge

FinBridge is a shared synthetic personal-finance and cross-border education-payment demo. Transaction categorization, balances, budgets, eligibility checks, and payment-policy decisions remain deterministic backend responsibilities. AI only classifies supported chat requests or suggests fields from a bill image; it cannot save a transaction, change balances, approve, or execute a payment.

FinBridge has no real bank connection, recipient verification, or money movement. The shared hosting profile has no login and uses an in-memory H2 database; restarting it returns the workspace to seeded data.

## Gemini-only AI

Gemini is the only model provider in the current build. The chatbot and bill/receipt extraction use Gemini from the backend; the browser never chooses a model or receives an API key.

- The assistant uses Gemini only to classify a supported chat request.
- A selected receipt or bill image can be sent to Gemini to suggest review-form fields.
- Gemini output is untrusted. Backend validation, the School Registry match, Policy Guard, approval, idempotency, calculations, and Sandbox execution remain deterministic server responsibilities.
- Gemini cannot verify a recipient, save a transaction by itself, create or approve a payment, execute a payment, or bypass a policy.

For chat routing, Gemini receives the current message as typed plus only server-derived, closed enum context: topic, channel, last intent, and pending state. It receives no transcript, financial facts, IDs, notification text, account or recipient data. Document extraction receives only the selected image and a fixed extraction instruction.

The label **Matched against simulated School Registry** means the beneficiary fields match seeded synthetic registry data. It is not real school, bank, account-owner, or invoice verification.

## Run locally with Gemini

Requirements: JDK 21, Maven 3.9+, and a `GEMINI_API_KEY` already set in the PowerShell session. Do not put the key in source control or browser code.

```powershell
cd D:\personal-finance-cross-border-agent
.\scripts\start-gemini.cmd -Port 8080
```

The `.cmd` launcher works when direct PowerShell scripts are blocked by execution policy. It starts the `gemini` Spring profile, enables Gemini for chat routing, and prints the local URL. Startup makes no model request.

| Setting | Default |
| --- | --- |
| `GEMINI_API_KEY` | Empty; required when Gemini is enabled |
| `FINBRIDGE_LLM_MODEL` | `gemini-3.5-flash-lite` |
| `FINBRIDGE_DOCUMENT_AI_MODEL` | `gemini-3.5-flash-lite` |
| `FINBRIDGE_LLM_CONNECT_TIMEOUT` | 3 seconds locally; 10 seconds in hosting |
| `FINBRIDGE_LLM_REQUEST_TIMEOUT` | 30 seconds |
| `APP_DEMO_TOOLS_ENABLED` | `true` in the base and hosting demo profiles |

`APP_DEMO_TOOLS_ENABLED=false` hides demo-only controls, including the fixed synthetic receipt sample. That sample never calls Gemini and exists only to rehearse the same reviewed receipt flow when the model is unavailable.

## Demo scope

The default `competition` presentation projects Student Finance to the Vietnam-to-China storyline and three channels:

- Alipay Student Payment
- Bank A International Transfer
- Bank B Promotional Rate - reference only and not executable in the synthetic demo

`finbridge.demo.presentation=extension` restores retained extension records for internal demonstration. This is a display projection, not an access-control boundary.

Ollama/Qwen are not available in the current build; an offline provider remains a future option.

## Test

```powershell
mvn -B test
```

The test suite uses synthetic local data and HTTP stubs. The opt-in real Gemini smoke test is disabled unless `FINBRIDGE_GEMINI_LIVE_SMOKE=true`; it is not part of the normal regression gate.

Read [demo readiness](docs/DEMO_READINESS.md), [ADR-002](docs/decisions/ADR-002-ai-receipt-and-demo-scope.md), and [the shared-demo deployment guide](docs/SHARED_DEMO_DEPLOYMENT.md) before presenting or deploying.