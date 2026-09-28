# Personal Finance Cross Border Agent — Phase 3

The current `main` branch implements the deterministic demo through Phase 3. Phase 1 provides the synthetic profile, seed transactions, simulated bank events, normalization and transaction type detection. Phase 2 adds merchant categorization, confidence handling, Undo, dashboard, budgets and Proactive Feed. Phase 3 adds tuition planning for the fixed Vietnam to China, VND to CNY corridor.

The Phase 3 demo includes a 20,000 CNY tuition bill, School Registry recipient verification, channel eligibility, synthetic FX quotes, fee and markup breakdowns, landed cost, expected received amount, settlement timing, latest safe date and rankings by cheaper, faster or safer.

All data is synthetic. Real providers, real payments, LLM execution and later phases are outside this implementation.

## Run

Requirements: Java 21 and Maven 3.9+.

```powershell
mvn test
mvn spring-boot:run
```

Open http://localhost:8080. The default database is a local H2 file for offline use. To use PostgreSQL, create a database named `finance_demo` and run with the `postgres` Spring profile, setting `DATABASE_URL`, `DATABASE_USER` and `DATABASE_PASSWORD`.

If Maven is not on `PATH` on the current machine, use the full commands recorded in [PHASE_3_VERIFICATION.md](PHASE_3_VERIFICATION.md).

## Phase 3 demo

1. Open **Student finance** and confirm Minh Nguyen's fixed Vietnam to China corridor.
2. Review the 20,000 CNY tuition bill and verified School Registry recipient.
3. Select **Cheaper**. Bank A ranks first among eligible channels.
4. Select **Faster**. Alipay Student Payment ranks first.
5. Select **Safer**. Bank A ranks first.
6. Confirm Bank B Promotional Rate is marked unavailable and has no selection or execution action, even though its synthetic cost is lower.
7. Refresh the synthetic quotes and review their source, timestamp and five minute expiry.
8. Reset demo data to restore the default cheaper preference and fixed bill.

## Sample channels

| Channel | Eligibility | Planning behavior |
|---|---|---|
| Alipay Student Payment | Eligible | Can be ranked for planning |
| Bank A International Transfer | Eligible | Can be ranked for planning |
| Bank B Promotional Rate | Unavailable | Reference only; never selectable or executable |

All financial calculations use Java `BigDecimal`. Quote and channel data are deterministic synthetic inputs; no provider API or LLM is used.