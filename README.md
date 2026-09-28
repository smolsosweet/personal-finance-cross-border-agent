# Personal Finance Cross Border Agent — Phase 1

Phase 1 implements the transaction foundation only: a synthetic demo profile, 22 seed transactions, a simulated bank event, normalization, four transaction types, duplicate protection and a one-click reset. Cross Border payments, LLM execution and the Payment Sandbox are not part of this branch yet.

## Run

Requirements: Java 21 and Maven 3.9+.

    mvn test
    mvn spring-boot:run

Open http://localhost:8080. The default database is a local H2 file for offline use. To use PostgreSQL, create a database named `finance_demo` and run with the `postgres` Spring profile, setting `DATABASE_URL`, `DATABASE_USER` and `DATABASE_PASSWORD` as needed.

## Phase 1 demo

1. The page starts with Minh Nguyen's synthetic profile and 22 seeded transactions.
2. Click each Simulated Bank Event button. The ledger records Expense, Income, Internal Transfer and Refund separately.
3. Internal Transfer moves value from checking to savings. It does not create a second income or expense transaction.
4. Click Reset demo data. The profile, account balances and 22 seeded transactions are restored.

All bank events and balances are synthetic. No external bank or payment service is called.

## Data model

`demo_profile` stores the demo user, `financial_accounts` stores synthetic VND balances, `bank_events` records raw source references, and `transactions` stores normalized events with a unique fingerprint.
