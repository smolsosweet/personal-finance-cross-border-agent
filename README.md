# Personal Finance Cross Border Agent — Phase 4

The current `main` branch implements the deterministic demo through Phase 4.

- Phase 1: synthetic user, bank events, normalization and transaction type detection.
- Phase 2: merchant categorization, confidence handling, Undo, dashboard, budgets and Proactive Feed.
- Phase 3: fixed Vietnam to China tuition corridor, School Registry verification, eligible channel comparison and deterministic landed cost.
- Phase 4: conversation UI, structured action plans, permission modes, deterministic Policy Guard, multi-currency Payment Sandbox, receipts, idempotency, Audit Log and Emergency Stop.

All data and payments are synthetic. No real Alipay, bank or payment provider is connected. The conversation layer cannot execute payments, modify policy, invent rates or fees, or change an approved recipient.

## Run

Requirements: Java 21 and Maven 3.9+.

```powershell
mvn test
mvn spring-boot:run
```

Open http://localhost:8080. The default database is a local H2 file. PostgreSQL remains available through the `postgres` Spring profile.

If Maven is not on `PATH`, use the full commands in [PHASE_4_VERIFICATION.md](PHASE_4_VERIFICATION.md).

## Phase 4 happy path

1. Open **Student finance** and choose **Create Approval Mode plan** for Bank A.
2. Review the structured action plan, exact recipient, quote, fee impact and idempotency key.
3. Confirm that no sandbox transaction exists before approval.
4. Select **Approve exact plan & execute**.
5. Review the receipt: VND debit, VND conversion amount, fee deduction, CNY credit, transaction ID, channel and quote.
6. Select **Retry with same idempotency key** and confirm the same receipt returns without another balance change.

Tuition always requires Approval Mode, including when the general permission mode is Delegated.

## Safety paths

- Switch to **Delegated Mode** and create the 250,000 VND low-risk Emergency Fund action. It executes automatically only because the recipient is allowlisted and the action is within every deterministic policy limit.
- Select **Emergency Stop**. New actions are blocked with `AGENT PAUSED`.
- Select **Test malicious instruction**. The input is blocked with `UNTRUSTED INSTRUCTION`; policy and recipient remain unchanged.
- Bank B remains reference only and has no plan or execution action.
- Use **Reset demo data** to restore Approval Mode, active state, initial sandbox balances and an empty receipt history.

All financial calculations use Java `BigDecimal`. FX rates and fees are read from the stored synthetic quote and are revalidated by Policy Guard before execution.
