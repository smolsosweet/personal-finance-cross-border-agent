# UI business-state consistency — verification report

## 1. Root causes and baseline

Baseline: `ebd89ee`. The detailed investigation was saved in [ROOT_CAUSES.md](ROOT_CAUSES.md) before production edits.

| Requested defect | Before | Root cause / source | After targeted tests |
|---|---|---|---|
| D1: approval reminder after payment | FAIL when another session pays/cancels/archives; same-session path already passed | Backend completion is correct, but baseline `app.js:2709–2716` opened cached DOM without recomputing. Two shared-state browser regressions failed before notification edits. | PASS: opening bell fetches a read-only authoritative notification fragment, including exact receipt update or removal of cancelled/archived bill reminder |
| D2: newly saved bill jumps to comparison | Not reproduced through UI save endpoint; PASS | `PhaseOneController.java:409` uses `addExpenseForReview`; absent selection maps to stage 1 (`:81`). Internal fixture helper `addExpense` intentionally selects, unlike the UI endpoint. No new fix claimed. | PASS, explicit UI selection required before stage 3 in EN/VI |
| D3: cancelled/archived bill remains selected | FAIL: both operations threw for the only selected bill | `CrossBorderService.java:621,638,662–678` required another matched active bill before removal. No fallback existed, so removal/selection update never happened. | Both regression tests PASS: empty selection and stage 1; multi-bill fallback still works |
| D4: historical receipt shortcut after another session changes bill | FAIL in integration and Playwright | `SessionConversationService.java:180` delegated visibility to `validate`; immutable completed plan fast path (`:220`) bypassed workspace check (`:223`). Historical validity and current shortcut visibility were conflated. | Both tests PASS; receipt and conversation history preserved |
| D5: seen control / newest-first ordering absent | Not reproduced; existing tests PASS | `app.js:2202–2291` already implements browser-local seen revision and descending recorded timestamp ordering. No new fix claimed. | PASS: order stable across reload/language; seen does not resolve tasks or alter financial snapshot |

Baseline integration + existing notification browser command:

```powershell
mvn '-Dtest=UiBusinessStateIntegrationTest,NotificationCenterPlaywrightTest' test
```

Actual: 19 tests, 16 passed, 1 assertion failure (D4), 2 errors (D3 cancel/archive), 0 skipped. This expected red run reproduced the defects before edits.

Additional browser baseline: old-receipt regression failed with expected count 0 / actual 1. The final bilingual walkthrough baseline, after correcting the NEW test's repeated dialog handler, mobile tab selector and expected existing Vietnamese wording, passed 1/1. Those test-authoring errors were not application defects; no existing tests or test infrastructure were changed.

## 2. Files changed

- `CrossBorderService.java`: cancel/archive permit no fallback; edit-time fallback validation retains its existing behavior. Recipient match, quote calculations and pending-plan invalidation semantics unchanged.
- `SessionConversationService.java`: current receipt/plan shortcut additionally requires unchanged workspace bill/account. Historical query validation remains unchanged.
- `PhaseOneController.java`: read-only `/notifications` fragment endpoint; home and refresh reuse the identical notification presentation rules.
- `app.js`: refresh notification fragment each time the bell opens; keep existing seen/order logic; retain last snapshot with a warning if refresh fails.
- `home.html`: named notification fragment and accessible freshness warning placeholder.
- `UiBusinessStateIntegrationTest.java`: eight isolated H2 regressions for completion, selection, remove/fallback, cross-session shortcut, stop and reset/replay.
- `UiBusinessStatePlaywrightTest.java`: six browser tests; actual local UI walkthrough EN/VI, recorded ordering/seen, cross-session stale shortcut, teammate payment/cancel/archive freshness, and failure fallback without retries.
- `docs/ui-business-state/ROOT_CAUSES.md`: baseline evidence recorded before edits.
- `docs/ui-business-state/VERIFICATION.md`: this report.
- `docs/ui-business-state/evidence/`: selected synthetic Playwright screenshots, retained outside target.
- `docs/ui-business-state/TEST_RESULTS.json`: sanitized test result totals and commands.

## 3. Additive storage changes

None. Existing browser-local `finbridge-notifications-seen-v1` is reused. No schema, financial tables, server persistence or dependency changes. Marking seen affects only emphasis/unread count; unresolved action-required items remain discoverable.

## 4. User-visible strings

One new warning pair, proposed for team copy approval: **EN:** "Could not refresh notifications. Showing the last loaded state; close and reopen to try again." **VI:** "Không làm mới được thông báo. Đang hiển thị trạng thái lần tải trước; đóng và mở lại để thử lại."

Existing `Mark as seen`, `Payment completed`, selection wording, business status values and reason codes remain unchanged. The warning appears only if the local read-only refresh fails; previous action-required items remain available and are explicitly described as the last loaded snapshot.

## 5. Verification

Targeted command:

```powershell
mvn '-Dtest=UiBusinessStateIntegrationTest,UiBusinessStatePlaywrightTest,NotificationCenterPlaywrightTest,NotificationRiskIntegrationTest,StudentExpenseIntegrationTest,AssistantPanelPlaywrightTest,PaymentWorkflowIntegrationTest,UiReviewFollowupPlaywrightTest' test
```

Actual: **75 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. The earlier targeted run before the shared-state notification fix passed 67/67.

Browser walkthrough: Chrome automation on local `8176`, isolated `jdbc:h2:mem:ui_state_browser`, model disabled/stubbed; EN desktop 1366px and VI mobile emulation 390px. Created a matched bill via the actual form; stage 1 until clicked; stage 3 after explicit selection; draft awaiting approval and zero payments; explicit approval yields exact Sandbox receipt; notification changes to informational completed update with receipt reference. Mark seen leaves financial snapshot unchanged. Repeated with cancel/archive and confirmed removed bills no longer selected/action-required. Old shortcut disappears on context change; historical messages/receipt remain.

Existing notification browser tests additionally cover 360/390/768/1366/1920px, timestamps, filters, actual VCB payment completion, storage unavailability and EN/VI. Integration checks Stop shows blocked-plan notification, reset removes stale plan references, and replay returns completion state.

Full suite first run (before adding the shared-state notification refresh): 395 tests, 1 assertion failure, 0 errors, 1 intentionally skipped live smoke. `UiReviewFollowupPlaywrightTest.launcherNeverStealsPaginationTaps` reported a hit-target ratio of 0.2 at 768px. The unchanged class subsequently passed 4/4 in the targeted run. No test or CSS was weakened/changed to address that failure. Final full suite on completed code: **399 tests, 395 passed, 0 assertion failures, 3 screenshot-write errors, 1 intentional live-smoke skip; BUILD FAILURE**. The previous hit-target assertion passed in this final run. Each of the three screenshot-write cases was rerun alone, and each still errored when writing an image. Windows reports: "The requested operation cannot be performed on a file with a user-mapped section open". At that time these three checks were BLOCKED. On continuation, all three isolated retries passed without any production or infrastructure changes; see the recovery section below. The full invocation itself remains recorded as BUILD FAILURE. No live Gemini/API calls are permitted. Standard offline test selection excludes the pre-existing untracked screenshot collector; opt-in cloud `*IT` tests are not part of Surefire's normal offline suite.

## 6. Protected logic / status values

The production diff is bill-selection fallback, chat-shortcut presentation and read-only notification refresh. The controller moves the existing notification rules into a reused helper without changing those rules. `PhaseFourService`, Policy Guard, approval/execution/idempotency, audit semantics, `schema.sql`, LLM clients/parsers/prompts and dependencies have no diff. The recipient-matching and FX/fee/landed-cost blocks in `CrossBorderService` have no diff. All status values and reason codes are unchanged. No conversation/audit records were deleted to remove a shortcut.

## 7. Limits

D2 and D5 were already implemented on the baseline and were not reproduced; this task does not claim to newly fix them. D1 was reproduced for a page left open during teammate state changes; D3 and D4 were also reproduced and fixed. Notifications refresh when opened; this does not introduce background polling. A failed refresh retains a labeled last-loaded snapshot. Tests use synthetic local data only. Render and physical devices were not tested; mobile evidence is Chrome viewport emulation. No push, deploy, hosting/secret change or live model request.

### Final full-suite command and isolated retries

```powershell
$env:FINBRIDGE_GEMINI_LIVE_SMOKE='false'
mvn '-Dtest=Test*,*Test,*Tests,*TestCase,!ScreenshotEvidenceCollectorTest' test
mvn '-Dtest=BilingualLayoutAcceptancePlaywrightTest#allTabsAndChatFitBothLanguagesAcrossDesktopTabletAndMobile' test
mvn '-Dtest=UiCleanupDefectRegressionPlaywrightTest#progressLabelsStayReadableForApprovalAndDelegatedAtEveryTargetSize' test
mvn '-Dtest=UiProductRefinementPlaywrightTest#captureWorkspaceScreens' test
```

Each isolated retry: 1 test, 0 assertion failures, 1 screenshot-write error, 0 skips. Locked output paths on retries were `docs/notification-layout/evidence/student-1440-en.png`, `docs/ui-ux-defect-fixes/after/D1-DELEGATED-390-vi.png`, and `docs/ui-ux/after/transactions-768.png`. No test infrastructure or existing test was edited to bypass these errors.

**Original commit gate: BLOCKED at first report.** It is now released by the successful isolated retries documented below, following the task exception for Windows screenshot locks. No push/deploy.

### Generated evidence housekeeping

The suite regenerated historical screenshots. 174 tracked evidence files were restored from the task's pre-run backup. Five other images could not be restored because Windows held them open: `docs/ui-ux/after/assistant-360.png`, `assistant-390.png`, `assistant-768.png`, `dashboard-1440.png`, and `dashboard-360.png` (all in the same directory). Their original copies are preserved under `target/ui-business-state-original-evidence/` and additionally under `docs/ui-business-state/pre-run-evidence/`. These residual generated image changes are outside the production patch and must not be staged into its eventual commit. Existing untracked reviews and screenshot collector were preserved.
## Recovery on continuation, 10 October 2026

Without changing application code, test assertions or infrastructure, the three previously blocked test methods were each run alone again. Each returned **1 test, 0 failures, 0 errors, 0 skips; BUILD SUCCESS**:

- BilingualLayoutAcceptancePlaywrightTest.allTabsAndChatFitBothLanguagesAcrossDesktopTabletAndMobile (12:51:55 +07).
- UiCleanupDefectRegressionPlaywrightTest.progressLabelsStayReadableForApprovalAndDelegatedAtEveryTargetSize (12:52:35 +07).
- UiProductRefinementPlaywrightTest.captureWorkspaceScreens (12:53:48 +07).

Results remain separated: targeted 75/75 PASS; final full invocation 399 tests with 3 screenshot-write errors and 1 live-smoke skip; affected isolated retries 3/3 PASS on continuation. No clean full-suite invocation is claimed.

All **179/179** pre-existing tracked evidence files were subsequently restored from their pre-run originals. No generated image or pre-run evidence folder is staged for the local fix commit. Task screenshots remain local-only under docs/ui-business-state/evidence; pre-run backup files remain local-only. Existing unrelated untracked files are preserved. Commit gate is satisfied using the expressly allowed isolated retries; only this task's production source, two regression test classes and textual reports are included.