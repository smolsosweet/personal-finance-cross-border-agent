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
4. Create a tuition plan, review landed cost, fees, FX quote expiry and deadline risk, then approve the exact plan.
5. Review the Payment Sandbox receipt, transaction ID and Audit Log.
6. Send the malicious instruction to see `UNTRUSTED INSTRUCTION`, trigger Emergency Stop, and reset/replay the demo.

Tuition always requires Approval Mode, including when the general permission mode is Delegated. All financial calculations use Java `BigDecimal`.

## Verification

Run the full automated suite and the manual acceptance script described in [PHASE_5_VERIFICATION.md](PHASE_5_VERIFICATION.md). The manual script starts from reset data and verifies all ten acceptance steps plus offline fallback and reset/replay.
