# Personal Finance Cross Border Agent — Phase 5

The `main` branch implements the deterministic demo through Phase 5.

- Phase 1: synthetic user, bank events, normalization and transaction type detection.
- Phase 2: merchant categorization, confidence handling, Undo, dashboard, budgets and Proactive Feed.
- Phase 3: fixed Vietnam to China tuition corridor, School Registry verification, eligible channel comparison and deterministic landed cost.
- Phase 4: conversation UI, structured action plans, Approval Mode, Delegated Mode for low-risk actions, deterministic Policy Guard, multi-currency Payment Sandbox, receipts, idempotency, Audit Log and Emergency Stop.
- Phase 5: prompt-injection, recipient, channel, FX, deadline, limit and safe-balance guards, offline fallback, reset/replay and the five-minute demo acceptance flow.

All data and payments are synthetic. No real Alipay, bank, payment provider or LLM execution is connected. The LLM boundary remains read-only: it cannot execute payments, modify policy, invent rates or fees, or change a recipient after approval.

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

## Contextual payment workflow

The payment screen opens after a plan is created. Its history entry then becomes available for reopening plans. Bill/beneficiary/quote snapshots remain immutable; identical pending requests reuse a plan, replacement plans revoke old approvals, and every execution rechecks Policy Guard. Unexecuted plans can be canceled; completed payments keep their original receipt.

See [the workflow and targeted verification report](docs/PAYMENT_WORKFLOW.md) for the Vietnamese guide, exact test commands, observed results and verification limitations.
