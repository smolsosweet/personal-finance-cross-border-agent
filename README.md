# Personal Finance Cross Border Agent — Phase 2

The current `main` branch implements the Personal Finance foundation through Phase 2. It includes a synthetic demo profile, seed transactions, simulated bank events, transaction normalization and type detection from Phase 1, plus deterministic merchant categorization, confidence handling, Undo, a transaction dashboard, monthly budget summary and Proactive Feed.

Cross Border, real bank integrations, LLM execution, permission decisions and Payment Sandbox are not implemented yet.

## Run

Requirements: Java 21 and Maven 3.9+.

```powershell
mvn test
mvn spring-boot:run
```

Open http://localhost:8080. The default database is a local H2 file for offline use. To use PostgreSQL, create a database named `finance_demo` and run with the `postgres` Spring profile, setting `DATABASE_URL`, `DATABASE_USER` and `DATABASE_PASSWORD` as needed.

If Maven is not on `PATH` on the current machine, use the full command recorded in [PHASE_2_VERIFICATION.md](PHASE_2_VERIFICATION.md).

## Phase 2 demo

1. The dashboard starts with Minh Nguyen's synthetic profile and 22 seeded transactions.
2. Click **High confidence**. Highlands is categorized as Food & Drinks at 97% and offers Undo.
3. Click **Medium confidence**. Campus Store opens a confirmation popup at 72%.
4. Click **Low confidence**. Unknown QR Merchant asks for its purpose at 35%.
5. Confirm or edit a category and review the updated budget and Proactive Feed.
6. Click **Reset demo data** to restore the profile, account balances, budgets and 22 seed transactions.

## Confidence behavior

| Confidence | Backend state | User experience |
|---|---|---|
| 90–100% | `AUTO` | Category is applied and Undo is available |
| 60–89% | `CONFIRMATION_REQUIRED` | Suggested category opens for confirmation |
| Below 60% | `PURPOSE_REQUIRED` | No category is assumed; the user supplies a purpose |

All rules, balances, budget totals and feed evidence are deterministic Java and SQL calculations. No LLM is used in Phase 2.
