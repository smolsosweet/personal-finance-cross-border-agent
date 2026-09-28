# Phase 2 Verification

Verified on 29 September 2026 with Java 21 and Spring Boot 3.5.6.

## Automated tests

Command:

```powershell
& 'C:\Users\ADMIN\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4ttckcbc\apache-maven-3.9.11\bin\mvn.cmd' '-Dmaven.repo.local=C:\Users\ADMIN\.m2\repository' test
```

Actual result:

```text
CategorizationServiceTest: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
PhaseTwoIntegrationTest: Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
TransactionServiceTest: Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
Total: Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The suite checks the exact 90, 89, 60 and 59 percent boundaries; deterministic high, medium and low rules; confirmation; purpose input; Undo; monthly budget allocation; grounded Proactive Feed; and all retained Phase 1 behavior.

## Application startup

The final verification instance used an isolated in-memory H2 database:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:h2:mem:manual_phase2_final;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1'
& 'C:\Users\ADMIN\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4ttckcbc\apache-maven-3.9.11\bin\mvn.cmd' '-Dmaven.repo.local=C:\Users\ADMIN\.m2\repository' spring-boot:run '-Dspring-boot.run.arguments=--server.port=8097'
```

Result: Spring Boot started successfully on port 8097 and the dashboard returned HTTP 200.

## Manual acceptance results

```text
START http=200 records22=True dashboard=True budget=True feed=True
HIGH confidence97=True auto=True category=True undo=True
UNDO purposeRequired=True
MEDIUM confidence72=True popup=True
MEDIUM_CONFIRM groceries=True confirmed=True
LOW confidence35=True purposePopup=True
LOW_CONFIRM purposeSaved=True records25=True
RESET records22=True
```

## Known issues

- The deterministic merchant map intentionally covers only the demo merchants. Unknown merchants require user input.
- User confirmations do not create reusable personal merchant rules yet.
- Budgets are fixed synthetic monthly limits and cannot be edited in Phase 2.
- Refunds are displayed separately but are not linked back to an original expense or subtracted from a category budget.
- Maven is available in the local cache but is not currently on the machine's `PATH`.

## Definition of Done

Phase 2 Definition of Done is satisfied for the requested scope. All three confidence paths work, the exact thresholds are tested, Undo works, dashboard and budget figures come from stored data, Proactive Feed includes deterministic evidence, and the retained Phase 1 tests still pass. No Phase 3 feature is included.
