# FinBridge demo readiness

## What this is

FinBridge is a shared synthetic demo, not a production financial service. The hosted `hosting` profile uses an in-memory H2 database, has no login, and returns to seed after a server restart. A page reload does not reset it. Reset, Sandbox approval, and Emergency Stop affect the shared workspace and require team coordination.

No real bank connection, recipient verification, bank-account ownership check, or money movement is implemented. Payment Sandbox receipts are simulated and immutable within the active runtime.

## Current provider and local launch

Gemini is the only current AI provider. Ollama/Qwen are not available in the current build; an offline provider remains a future option only.

For local Gemini verification, set `GEMINI_API_KEY` in the PowerShell session, then run:

```powershell
.\scripts\start-gemini.cmd -Port 8080
```

The launcher starts profile `gemini`, enables intent routing, and does not issue a model request at startup. The normal automated test suite uses stubs and does not require a key. `GeminiProviderLiveSmokeTest` is opt-in through `FINBRIDGE_GEMINI_LIVE_SMOKE=true` and is excluded from an ordinary regression run.

The hosted Docker image starts profile `hosting`. Its configuration expects `GEMINI_API_KEY` as a hosting secret, sets `FINBRIDGE_LLM_ENABLED=true`, and uses a 10-second connection timeout and 30-second request timeout. Never put a key in source, a URL, browser JavaScript, or a committed `.env` file.

## Safety boundaries

- Gemini classifies supported requests and may suggest review-form fields from a selected image.
- Gemini output remains untrusted. Server code validates inputs and owns financial data, policy, calculations, registry matching, approval, idempotency, audit and Sandbox execution.
- A model request cannot verify a recipient, create a payment, approve a payment, execute a payment, or change a balance directly.
- The recipient label means only that seven fields match the seeded **Simulated School Registry**.
- The fixed synthetic receipt sample is available only when `APP_DEMO_TOOLS_ENABLED=true`. It issues the same server capability as a successful receipt extraction, then uses the normal review/save/link/override flow. It does not call Gemini and does not insert an expense when opened.

## Accepted scope and known limits

- Competition presentation shows Vietnam-to-China, VND-to-CNY tuition and three retained channels: Alipay Student Payment, Bank A International Transfer, and non-executable Bank B Promotional Rate. Set `finbridge.demo.presentation=extension` to display retained extension data.
- TC07 receipt linking uses a configurable date window. The default is `ReceiptLinkService.DEFAULT_DATE_WINDOW_DAYS = 3`; valid configuration is `finbridge.receipt.date-window-days` from 0 to 31.
- A session receipt capability proves that a reviewed image/sample originated in that browser session and that an explicit choice was made. It does not prove user ownership because the demo has no authentication and shares data.
- Ordinary manually entered same-merchant, same-amount, same-day transactions still collapse under the pre-existing dedupe rule. School-bill-to-historic-expense matching and cross-currency matching remain roadmap work.
- The 768px pagination layout test has a known intermittent test issue. In the observed failure, its probe point was `y=878.95` below an `844px` viewport and `elementFromPoint` returned `null`; no launcher overlap was demonstrated. It affects only this tablet-width automated layout check. See [Package 4 evidence](pre-submission/PACKAGE_4_BLOCKED.md).

## Before a team demo

1. Confirm the deployed commit shown by the hosting provider.
2. Open the deployed URL in both English and Vietnamese, then verify one read-only chat response and one draft that remains awaiting approval.
3. Agree a test window before resetting shared data, approving a Sandbox draft, or toggling Emergency Stop.
4. Keep a local or recorded fallback for hosting cold starts, provider quota, timeout, or service errors.

Physical-device mobile testing and a manual pass in both languages on the deployed app remain human checks; browser viewport automation is not a substitute.

## Automated evidence

Local package gates before the final regression:

| Package | Commit | Evidence |
| --- | --- | --- |
| Minimal Gemini enum context | `d650cc4` | 89 targeted tests passed |
| TC07 reviewed receipt linking | `f00561d` | 46 targeted tests passed |
| Competition presentation | `fb4839b` | 79 targeted tests passed |
| Generated evidence isolation | `e32a874` | Three former screenshot-lock tests each passed independently |

The final offline regression report is added only after it runs. It must state actual failures, skips, and blockers rather than claiming a fully green suite by default.