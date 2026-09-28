# Phase 1 Verification

Verified on 29 September 2026 with Java 21 and Spring Boot 3.5.6.

## Scope

Phase 1 contains the demo user profile, synthetic seed data, simulated bank events, transaction normalization, Expense, Income, Internal Transfer and Refund detection, duplicate protection and demo reset. Cross Border, real bank integrations, LLM execution and Payment Sandbox are not implemented.

## Automated tests

Command:

```powershell
& 'C:\Users\ADMIN\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4ttckcbc\apache-maven-3.9.11\bin\mvn.cmd' '-Dmaven.repo.local=C:\Users\ADMIN\.m2\repository' test
```

Result:

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The tests cover all four transaction types, normalization and invalid amounts, balance effects for an internal transfer, duplicate fingerprint handling and three consecutive reset cycles.

## Application startup

The verification instance used an isolated in-memory H2 database and port 8098:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:h2:mem:manual_phase1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1'
& 'C:\Users\ADMIN\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4ttckcbc\apache-maven-3.9.11\bin\mvn.cmd' '-Dmaven.repo.local=C:\Users\ADMIN\.m2\repository' spring-boot:run '-Dspring-boot.run.arguments=--server.port=8098'
```

Result: the application started successfully and served `http://localhost:8098/` with HTTP 200.

## Manual acceptance scenario

The application was reset, then the Expense, Income, Internal Transfer and Refund event buttons were exercised through their HTTP form endpoints.

Observed results:

```text
START http=200 records22=True profile=True checking100m=True savings1m=True
EVENT expense http=200
EVENT income http=200
EVENT transfer http=200
EVENT refund http=200
AFTER records26=True expense=True income=True internalTransfer=True refund=True simulatedLabel=True checking100700000=True savings1200000=True
RESET records22=True checking100m=True savings1m=True
```

This verifies the seed profile, initial balances, event processing, normalized ledger output, four transaction types, account effects and reset behavior.

## Known issues

- Maven is installed in the local Maven wrapper cache but is not currently available as `mvn` on `PATH`.
- `mvn clean` cannot delete the packaged JAR while another application process is using it. Stop running instances before a clean package build.
- The duplicate fingerprint uses account, direction, amount, merchant and a timestamp rounded to one minute. Two legitimate identical transactions within the same minute may be treated as duplicates.
- Transaction type detection is deliberately limited to Phase 1 demo rules and the two known synthetic accounts.

## Definition of Done

Phase 1 Definition of Done is satisfied for the agreed scope. Automated tests pass, the application starts, the primary acceptance flow works, reset is repeatable, and no Phase 2 feature is included.
