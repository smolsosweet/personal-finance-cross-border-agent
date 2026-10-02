# Personal Finance Cross Border Agent — Guarded AI Intent Providers

The `main` branch implements the deterministic demo through Phase 5 plus an optional guarded intent classifier using OpenAI or local Ollama.

- Phase 1: synthetic user, bank events, normalization and transaction type detection.
- Phase 2: merchant categorization, confidence handling, Undo, dashboard, budgets and Proactive Feed.
- Phase 3: fixed Vietnam to China tuition corridor, School Registry verification, eligible channel comparison and deterministic landed cost.
- Phase 4: conversation UI, structured action plans, Approval Mode, Delegated Mode for low-risk actions, deterministic Policy Guard, multi-currency Payment Sandbox, receipts, idempotency, Audit Log and Emergency Stop.
- Phase 5: prompt-injection, recipient, channel, FX, deadline, limit and safe-balance guards, offline fallback, reset/replay and the five-minute demo acceptance flow.

All data and payments are synthetic. No real Alipay, bank or payment provider is connected. When enabled,
the selected LLM provider classifies supported personal-finance and tuition intents through strict structured output. It cannot execute
payments, modify policy, invent rates or fees, or change a recipient after approval.

## Run

Requirements: Java 21 and Maven 3.9+.

```powershell
mvn test
mvn spring-boot:run
```

Open http://localhost:8080. The default database is a local H2 file. PostgreSQL remains available through the `postgres` Spring profile.

If Maven is not on `PATH`, use the full commands in [PHASE_5_VERIFICATION.md](PHASE_5_VERIFICATION.md).

## Five-minute demo

1. Review the seeded personal transaction and its automatic category.
2. Read the deterministic tuition insight and School Registry verification.
3. Compare Alipay Student Payment and Bank A International Transfer; Bank B is unavailable and cannot be executed.
4. Select a verified active bill and a connected channel, then Create plan opens its contextual payment review. Check beneficiary bank details, funding source, landed cost, remaining balance and quote countdown before approving.
5. Review the receipt and transaction ID for that exact plan. Open its Audit Log or reopen a paid bill through View receipt; paid bills cannot create another payment.
6. Expand Demo tools and policy settings to test malicious instructions, low-risk Delegated actions and offline fallback. Emergency Stop remains in the global header; reset/replay starts from the Dashboard.

Tuition always requires Approval Mode, including when the general permission mode is Delegated. All financial calculations use Java `BigDecimal`.

## Verification

Run the full automated suite and the manual acceptance script described in [PHASE_5_VERIFICATION.md](PHASE_5_VERIFICATION.md). The manual script starts from reset data and verifies all ten acceptance steps plus offline fallback and reset/replay.

## Assistant access

Use **Ask FinBridge** at the bottom-right of any tab. The side panel shares the existing Overview conversation, supports English/Vietnamese, and keeps drafts while switching tabs. Student finance and payment review also have contextual help buttons. Suggested questions are submitted only after Send; a payment draft must still be reviewed and explicitly approved on the payment screen.

See [the assistant UI guide and targeted test results](docs/ASSISTANT_WORKSPACE.md). The existing `APP_DEMO_TOOLS_ENABLED=false` option hides demo tools while retaining the assistant.

## Contextual payment workflow

The payment screen opens after a plan is created. Its history entry then becomes available for reopening plans. Bill/beneficiary/quote snapshots remain immutable; identical pending requests reuse a plan, replacement plans revoke old approvals, and every execution rechecks Policy Guard. Unexecuted plans can be canceled; completed payments keep their original receipt.

See [the workflow and targeted verification report](docs/PAYMENT_WORKFLOW.md) for the Vietnamese guide, exact test commands, observed results and verification limitations.

## Guarded AI intent configuration

The optional AI intent boundary is disabled by default. Provider selection is explicit; an API key by itself never enables a provider.

### Local Ollama with qwen3:4b

Install and start Ollama locally, then check and download the required model:

```powershell
ollama --version
ollama list
ollama pull qwen3:4b
```

Start FinBridge with Ollama:

```powershell
$env:FINBRIDGE_LLM_ENABLED="true"
$env:FINBRIDGE_LLM_PROVIDER="ollama"
$env:FINBRIDGE_LLM_MODEL="qwen3:4b"
$env:FINBRIDGE_LLM_BASE_URL="http://localhost:11434"
$env:FINBRIDGE_LLM_CONNECT_TIMEOUT="3s"
$env:FINBRIDGE_LLM_REQUEST_TIMEOUT="60s"
mvn spring-boot:run
```

`FINBRIDGE_LLM_MODEL` may be omitted for Ollama; it defaults to `qwen3:4b`. Keep Ollama bound locally and do not expose port 11434 publicly.

Check the local service and installed models without sending a prompt:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:11434/api/tags
```


With FinBridge running on port 8089 using the configuration above, run the opt-in browser smoke test that calls the real local model:

```powershell
mvn "-Dtest=OllamaLivePlaywrightIT" test
```

This live test is intentionally excluded from the default Maven test suite so normal builds do not depend on a local Ollama process.

### OpenAI

The existing OpenAI setup remains compatible. `openai` is the default provider value, but the enable flag, model and API key are still required:

```powershell
$env:OPENAI_API_KEY="<set locally; never commit>"
$env:FINBRIDGE_LLM_PROVIDER="openai"
$env:FINBRIDGE_LLM_MODEL="<a Structured Outputs capable model>"
$env:FINBRIDGE_LLM_ENABLED="true"
mvn spring-boot:run
```

To force the guided deterministic experience without any model request:

```powershell
$env:FINBRIDGE_LLM_PROVIDER="disabled"
$env:FINBRIDGE_LLM_ENABLED="false"
mvn spring-boot:run
```

Unknown provider values stop application configuration with a clear error. Provider timeout, connection failure, HTTP error, invalid JSON or schema mismatch produces the existing safe unavailable message. Fallback never creates a chat-driven payment plan; the guided deterministic tuition workflow remains usable.

The provider response selects only supported backend-owned personal-finance or tuition intents.
Cheapest/fastest preferences apply only to existing tuition actions; read-only insights require NONE.
Amounts, recipients, accounts, bills, quotes, currencies, corridors, approval and policy outcomes are loaded and
validated by deterministic backend services. Missing configuration, provider errors and invalid structured output
fall back without creating or executing a payment plan.


## Read-only AI Personal Finance

Ask in the Overview chat about spending in the current demo month, configured category budgets,
or the projected balance after the selected verified tuition bill. Ollama only classifies the
question; existing backend services calculate all amounts and backend templates explain their evidence.

Examples: "Where did I spend the most this month?", "Ngân sách tháng này còn bao nhiêu?",
and "Nếu đóng học phí thì còn đủ tiền sinh hoạt không?".

The scope is the current demo profile/month. Other periods, custom account/category scopes and
model-generated financial parameters require clarification. Refunds remain separate from gross
expenses; internal transfers are excluded, and currencies are never summed together. Budgets are
configured in VND. Tuition affordability is a synthetic projection using the selected Sandbox
funding account and its eligible, unexpired quote. Planner commitments are shown with an explicit
conservative assumption because personal planner and payment accounts are separate sources.

The chat shows processing, blocks duplicate submissions, restores controls after errors/timeouts,
and discards responses superseded by reset or a newer request. No automatic retry is performed.
Responses to the three read-only intents change only conversation and audit records.

Targeted checks:

    mvn "-Dtest=PersonalFinanceAiIntegrationTest,PersonalFinanceAiPlaywrightTest,FinBridgePlaywrightE2ETest,TransactionServiceTest" test

Separate live model and browser verification (requires installed qwen3:4b and running local Ollama;
starts its own synthetic FinBridge instance on port 8091):

    mvn "-Dtest=PersonalFinanceOllamaLiveIT" test

The default Maven test suite uses stubs for model responses and does not depend on Ollama.
See [the Vietnamese verification report](docs/AI_PERSONAL_FINANCE_VERIFICATION.md) for measured results and limitations.


## Demo preparation and full-story rehearsal

Run `./scripts/prepare-demo.ps1`, then ask one read-only budget question through FinBridge before presenting.
Reset synthetic application data if needed and begin the demo promptly; warming model weights alone does not
guarantee the first classifier request is fast. Keep the existing 60-second timeout and fallback.

Opt-in real-Ollama browser rehearsal (synthetic database, port 8093):

    mvn "-Dtest=DemoRehearsalOllamaLiveIT" test

See [the Vietnamese demo readiness guide](docs/DEMO_READINESS.md) for the workflow, focused test evidence
and the transaction timestamp precision fix.
