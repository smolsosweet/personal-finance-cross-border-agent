# Final pre-submission regression report

- Date: 2026-10-10
- Mode: local synthetic data, including Playwright; `FINBRIDGE_GEMINI_LIVE_SMOKE=false`
- External actions: no Gemini live request, push, deploy, Render reset, Sandbox approval, or Emergency Stop.

## Committed packages and targeted evidence

| Package | Commit | Evidence |
| --- | --- | --- |
| Minimal Gemini enum context | `d650cc4` | 89 tests passed |
| TC07 reviewed receipt linking | `f00561d` | 46 tests passed |
| Competition presentation and honest simulated-source wording | `fb4839b` | 79 tests passed |
| Generated evidence isolation | `e32a874` | Three former screenshot-lock tests each passed independently: 1/1, 1/1 and 1/1 |
| ADR, README and demo-readiness documentation | `2c91428` | 10 supporting tests passed |
| Stale simulated-source browser assertions | `fff0ab0` | 17 browser tests passed |
| Workspace browser-port isolation | `1546d58` | `ReceiptLinkPlaywrightTest` and `WorkspaceListScrollPlaywrightTest`: 7 tests passed together |

The final two test-only commits were made after the first full run exposed obsolete wording assertions and a shared fixed-port collision. Neither changes application behavior.

## Required TC07 evidence

The following tests passed in the Package 2 gate and again passed as part of the final suite before its unrelated pagination failure:

- `ReceiptLinkIntegrationTest#overrideDoubleClickReplayAndThirdIdenticalUploadAreSafe` verifies two identical real purchases through the explicit override: double click/replay creates one additional expense; a third identical submission asks again.
- `ReceiptLinkIntegrationTest#forgedForeignExpiredAndResetCapabilitiesNeverWriteFinance` verifies forged, foreign, stale and reset operation/capability paths produce zero financial writes.
- `ReceiptSampleGuardTest#sampleRouteAndButtonAreUnavailableWhenDemoToolsDisabled` verifies the fixed sample endpoint returns 404 and its button is absent when demo tools are disabled.

The fixed sample is enabled only by the server-side `app.demo-tools-enabled` property, mapped from `APP_DEMO_TOOLS_ENABLED`. It is enabled by the base shared-demo configuration and the `hosting` profile; it is unavailable when the property is false. It issues the same server capability as receipt extraction and is not an AI bypass.

## Final full regression

The tracked-test-only command was run with live Gemini disabled:

```powershell
$trackedTests = git ls-files 'src/test/java/**/*Test.java' | ForEach-Object { $_ -replace '^src/test/java/','' -replace '/', '.' -replace '\.java$','' }
$env:FINBRIDGE_GEMINI_LIVE_SMOKE='false'
mvn -B ("-Dtest=" + ($trackedTests -join ',')) test
```

Actual result: **433 tests; 1 failure; 0 errors; 1 skipped; BUILD FAILURE**.

- The skipped test was `GeminiProviderLiveSmokeTest`, intentionally disabled by `FINBRIDGE_GEMINI_LIVE_SMOKE=false`.
- The only failure was `UiReviewFollowupPlaywrightTest#launcherNeverStealsPaginationTaps` at the 768px tablet case. The required isolated rerun was executed once and passed: **1 test; 0 failures/errors/skips; BUILD SUCCESS**.
- Therefore the full suite must not be described as fully green.

## Known intermittent issue

The failed full-suite assertion probed `y=878.95` while the viewport height was `844`; `elementFromPoint` returned `null`. No overlap with the assistant launcher was demonstrated. The leading hypothesis is an unproven smooth-scroll/layout synchronization race around `app.js:1166`. The original assertion and CSS remain unchanged. This affects only the automated 768px tablet layout check.

`PaginationGeometryDiagnosticTest.java` remains deliberately untracked and uncommitted. It was not modified to make the test pass. The unrelated untracked `ScreenshotEvidenceCollectorTest.java` and team-review artifacts remain unstaged.

## Approval constants and user-visible strings

Team sign-off is still required for:

1. `ReceiptLinkService.DEFAULT_DATE_WINDOW_DAYS = 3`, with permitted configuration from 0 through 31 days.
2. The default Vietnam-to-China channel set: **Alipay Student Payment**, **Bank A International Transfer**, and **Bank B Promotional Rate** (reference only).

Package 2 records all added receipt-review English/Vietnamese pairs, including Receipt review/Xem lại biên lai, Linked expense/Khoản chi đã liên kết, This is a different purchase/Đây là khoản chi khác, and Use synthetic sample receipt/Dùng biên lai mẫu mô phỏng. Package 3 records all changed simulated-source English/Vietnamese pairs, including `Reference only: no linked Bank B source in the simulation` / `Chỉ tham khảo: chưa liên kết nguồn Bank B trong mô phỏng`. See `PACKAGE_2.md` and `PACKAGE_3.md` for the complete reviewed dictionaries.

## Protected boundaries

`git diff -- src/main` is empty after the committed work. The final corrective commits change test code only. Policy Guard, approval/execution, Sandbox, schema, calculations, recipient-match logic, Gemini provider behavior, and request budgets/counters have no uncommitted diff.

## Human completion items

1. Confirm the deployment provider shows the intended commit as Live.
2. Manually exercise the deployed application in Vietnamese and English, including a physical-phone pass if mobile support is claimed.
3. Coordinate the shared workspace before any reset, Sandbox approval, or Emergency Stop.
4. Treat the 768px layout case as a known intermittent test until separately investigated; do not infer a launcher overlap from the existing evidence.