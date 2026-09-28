# Phase 3 Verification

Date: 2026-09-29

## Scope

Phase 3 implements only the fixed Vietnam to China, VND to CNY tuition planning corridor for a 20,000 CNY bill. It includes the student profile, bill, School Registry recipient verification, channel eligibility, FX quote metadata, fee and markup calculations, landed cost, expected received amount, settlement timing, latest safe date, and preference ranking.

No real Alipay or bank integration, payment execution, Payment Sandbox, or later phase was added.

## Automated tests

Exact command:

```powershell
& 'C:\Users\ADMIN\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4ttckcbc\apache-maven-3.9.11\bin\mvn.cmd' '-Dmaven.repo.local=C:\Users\ADMIN\.m2\repository' test
```

Result:

- `TransactionServiceTest`: 5 passed
- `CategorizationServiceTest`: 3 passed
- `PhaseTwoIntegrationTest`: 6 passed
- `PhaseThreeIntegrationTest`: 7 passed
- Total: 21 passed, 0 failures, 0 errors, 0 skipped
- Maven result: `BUILD SUCCESS`

Phase 3 tests cover the fixed corridor and bill, recipient match and mismatch, all channel eligibility states, Bank B exclusion, exact `BigDecimal` values, quote metadata and expiry, settlement dates, every preference ranking, refresh and reset.

## Application startup

Exact command:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:h2:mem:manual_phase3;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1'
& 'C:\Users\ADMIN\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4ttckcbc\apache-maven-3.9.11\bin\mvn.cmd' '-Dmaven.repo.local=C:\Users\ADMIN\.m2\repository' spring-boot:run '-Dspring-boot.run.arguments=--server.port=8096'
```

Result: Spring Boot 3.5.6 started successfully with Java 21.0.6 on port 8096 using isolated in-memory H2 data.

## Manual acceptance scenario

Exact command while the application is running:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-phase3.ps1 -BaseUrl http://localhost:8096
```

Actual result:

```text
GET / status 200: PASS
Phase 3 UI: PASS
Vietnam to China: PASS
Tuition 20,000 CNY: PASS
School registry verified: PASS
Alipay shown: PASS
Bank A shown: PASS
Bank B unavailable: PASS
Bank B no execute: PASS
Cheaper ranks Bank A first: PASS
Faster ranks Alipay first: PASS
Safer ranks Bank A first: PASS
Quote refresh redirects home: PASS
Reset restores cheaper preference: PASS
Reset preserves fixed demo bill: PASS
```

The application was stopped after verification.

## Changed files

- `README.md`
- `PHASE_3_VERIFICATION.md`
- `scripts/verify-phase3.ps1`
- `src/main/java/com/example/finance/CrossBorderService.java`
- `src/main/java/com/example/finance/DemoDataService.java`
- `src/main/java/com/example/finance/PhaseOneController.java`
- `src/main/resources/schema.sql`
- `src/main/resources/static/app.css`
- `src/main/resources/templates/home.html`
- `src/test/java/com/example/finance/PhaseThreeIntegrationTest.java`

## Known issues

- Quote rates, fees, eligibility and safety scores are synthetic fixtures, not live provider data.
- Settlement days use calendar days and do not model weekends, holidays or bank cutoff times.
- Expected received amount assumes the displayed sender side fees and markup; intermediary or recipient deductions are not modeled.
- The safety score is a fixed demo value, not a production risk assessment.
- Quote expiry is displayed and flagged after five minutes; Phase 3 has no payment execution to authorize or block.

## Definition of Done

Satisfied for Phase 3:

- Every requested Phase 3 capability is implemented.
- Financial calculations use `BigDecimal`.
- Bank B remains unavailable and is excluded from eligible options despite its lower synthetic rate.
- All automated tests pass.
- The application starts successfully.
- The main acceptance scenario passes.
- No real provider integration or later phase was implemented.