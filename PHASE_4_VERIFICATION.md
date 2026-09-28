# Phase 4 Verification

Date: 2026-09-29

## Scope

Phase 4 adds the conversation experience, structured action plans, Approval Mode, Delegated Mode for low-risk actions, deterministic Policy Guard, multi-currency Payment Sandbox, VND debit, VND to CNY conversion, fee deduction, CNY credit, receipt and transaction ID, idempotency, Audit Log and Emergency Stop.

Tuition payments always require Approval Mode. Conversation input cannot call the sandbox executor, change policy or recipient, or provide FX rates and fees.

No real payment provider, LLM execution or Phase 5 capability was added.

## Automated tests

Exact command:

```powershell
& 'C:\Users\ADMIN\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4ttckcbc\apache-maven-3.9.11\bin\mvn.cmd' '-Dmaven.repo.local=C:\Users\ADMIN\.m2\repository' test
```

Actual result:

- `CategorizationServiceTest`: 3 passed
- `TransactionServiceTest`: 5 passed
- `PhaseTwoIntegrationTest`: 6 passed
- `PhaseThreeIntegrationTest`: 7 passed
- `PhaseFourIntegrationTest`: 11 passed
- Total: 32 passed, 0 failures, 0 errors, 0 skipped
- Maven result: `BUILD SUCCESS`

Phase 4 tests cover grounded conversation output, structured plans, mandatory tuition approval, exact BigDecimal receipt values, four ledger stages, idempotent retry, both permission modes, delegated limits, unavailable channel, expired quote, approval invalidation after changed data, Emergency Stop, prompt injection blocking and three consecutive reset/replay cycles.

## Application startup

Exact command:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:h2:mem:manual_phase4_final;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1'
& 'C:\Users\ADMIN\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4ttckcbc\apache-maven-3.9.11\bin\mvn.cmd' '-Dmaven.repo.local=C:\Users\ADMIN\.m2\repository' spring-boot:run '-Dspring-boot.run.arguments=--server.port=8095'
```

Result: Spring Boot 3.5.6 started successfully with Java 21.0.6 on port 8095 using isolated in-memory H2 data.

## End-to-end acceptance verification

Exact command while the application is running:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-phase4.ps1 -BaseUrl http://localhost:8095
```

Actual result: 27 checks passed.

```text
GET / status 200: PASS
Phase 4 UI rendered: PASS
Conversation UI rendered: PASS
Structured plan UI rendered: PASS
Policy Guard and sandbox rendered: PASS
Bank B has no execution action: PASS
Only two eligible plan buttons: PASS
Grounded conversation uses demo surplus: PASS
Tuition action ID created: PASS
Tuition remains Approval Mode: PASS
No execution before approval: PASS
Approval creates transaction ID: PASS
VND debit is exact: PASS
Conversion amount is exact: PASS
Fee deduction is exact: PASS
CNY credit is exact: PASS
Receipt includes channel and quote: PASS
Audit records sandbox execution: PASS
Idempotent retry returns same transaction: PASS
Idempotent retry is audited: PASS
Delegated low-risk action auto executes: PASS
Delegated receipt uses VND credit: PASS
Emergency Stop blocks new action: PASS
Prompt injection is blocked: PASS
Prompt injection does not change mode: PASS
Reset restores Approval Mode and active state: PASS
Reset clears sandbox transaction: PASS
```

The application was stopped after verification.

## Changed files

- `README.md`
- `PHASE_4_VERIFICATION.md`
- `scripts/verify-phase4.ps1`
- `src/main/java/com/example/finance/CrossBorderService.java`
- `src/main/java/com/example/finance/DemoDataService.java`
- `src/main/java/com/example/finance/PhaseFourService.java`
- `src/main/java/com/example/finance/PhaseOneController.java`
- `src/main/resources/schema.sql`
- `src/main/resources/static/app.css`
- `src/main/resources/templates/home.html`
- `src/test/java/com/example/finance/PhaseFourIntegrationTest.java`

## Known issues

- Payment channels, balances, rates, fees, recipients and receipts are synthetic.
- Conversation responses are deterministic offline responses; no external LLM is integrated.
- Sandbox ledger entries demonstrate the required stages but are not a production double-entry accounting system.
- Audit records are append-only through application behavior but are not cryptographically signed or exported.
- Sequential retries are idempotent; concurrent duplicate requests rely on database uniqueness and are not returned as a friendly existing receipt response.
- Daily policy limits use the application host calendar and do not implement a separate user timezone policy.

## Definition of Done

Satisfied for Phase 4:

- Every requested Phase 4 capability is implemented.
- Tuition always requires explicit approval.
- Delegated Mode only automatically executes low-risk, allowlisted actions within deterministic limits.
- Policy Guard runs before every sandbox execution.
- Rates, fees and recipient are revalidated from trusted stored data.
- The sandbox produces VND debit, conversion, fee deduction, destination credit, receipt and transaction ID.
- Retry does not duplicate balance changes.
- Successful and blocked actions appear in Audit Log.
- Emergency Stop immediately blocks new actions.
- All automated tests pass.
- The application starts successfully and the end-to-end acceptance scenario passes.
- Phase 5 was not started.
