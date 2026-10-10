# Package 4 — generated evidence isolation and deferred tablet pagination check

Packages 1–3 remain committed locally:
- d650cc4 — minimal server enum Gemini context policy, 89 targeted tests passed.
- f00561d — TC07 reviewed receipt linking + approved narrow server override identity + fixed offline seed sample, final gate 46 passed.
- fb4839b — Vietnam/China competition display + honest source wording, final gate 79 passed. This amended the earlier Package 3 hash solely to correct the report timestamp from the actual test log.

No push/deploy/Render mutation/live Gemini request occurred.

## Evidence relocation (implemented, NOT committed)
A shared TestEvidence helper places generated images in target/test-evidence/<random UUID>/, a fresh namespace per test JVM. Paths are relative and checked to stay within that root. Tracked screenshot-producing tests now use it, including old docs paths and fixed target screenshot names. Existing committed PNGs are untouched. The helper's confinement test passed. No existing assertion, CSS, application source or request budget/counter was changed in Package 4.

The unrelated, pre-existing untracked ScreenshotEvidenceCollectorTest and team-review documents were not modified or staged. The collector still writes its own untracked team-review directory; it is not a released repository test. Its generated output must not be staged.

## Deferred known intermittent issue
| Run | Original launcherNeverStealsPaginationTaps result | Actual evidence |
|---|---|---|
| 1 | PASS (1/1); TestEvidenceTest also PASS (1/1) | target/pre-submission-package4-repeat-1.log, BUILD SUCCESS, 14:27:41 +07 |
| 2 | PASS (1/1) | target/pre-submission-package4-repeat-2.log, BUILD SUCCESS, 14:29:12 +07 |
| 3 | FAIL (1 assertion failure) | target/pre-submission-package4-repeat-3.log, BUILD FAILURE, 14:29:27 +07 |
| 4 | Not run | Deferred by the superseding Package 4 decision |
| 5 | Not run | Deferred by the superseding Package 4 decision |

Failure: student-bill hit target at 768 ratio 0.2, expected true, actual false.
Original assertions and CSS are preserved. This is the same location the PM had flagged earlier. It is NOT a PNG write/lock error.

Original failure screenshot:
target/test-evidence/70245f89-7325-4add-9ca3-e35c5e660eb9/docs/ui-ux-followup/after/student-bill-768.png

## Read-only geometry investigation
Opt-in PaginationGeometryDiagnosticTest reproduces local synthetic fixtures and observes DOM positions/hit elements. It does not alter app scrolling, CSS or the original assertions, and makes zero model calls. Run with:
mvn -Dtest=PaginationGeometryDiagnosticTest -Dfinbridge.pagination.diagnostic=true test

First diagnostic had extra before/after evaluations and omitted the shared-demo banner: eight traversals did NOT reproduce. It is supporting observation only, not an acceptance PASS.

The second diagnostic matched the original shared-workspace/demo-tools fixture and removed the extra pre-position reads; reproduced in traversal 1:
- viewport: width 768, height 844
- scrollY: 736
- Next button rect: x 470.984375, y 859.953125, width 52.015625, height 38
- 0.2 point: x 481.3875, y 878.953125, elementFromPoint=null
- 0.5 point: x 496.9921875, y 878.953125, elementFromPoint=null
- 0.8 point: x 512.596875, y 878.953125, elementFromPoint=null

Evidence: target/pre-submission-package4-geometry-exact.log and
target/test-evidence/02fc49dd-702e-452a-aa0e-85c557663209/pagination-geometry/geometry.json

The diagnostic itself completing successfully does NOT mean the original acceptance check passed: it captured the failure.

**Known intermittent issue:** the test probes points below the visible viewport, so it cannot establish that the launcher stole the tap. No hit element was the launcher. `activateTab` calls asynchronous smooth scroll to top (`app.js:1166`), then this test waits only for button enabled, immediately positions using instant scroll and takes a screenshot. Scroll/layout synchronization is the leading explanation; the exact race is not claimed proven. A genuine launcher overlap at a settled viewport is not demonstrated by this failure. It affects only the 768px tablet-width layout check and is deferred without changing its assertions, CSS, execution status, or test source.

## Evidence relocation acceptance
The following three screenshot-lock recovery checks passed independently after generated image paths moved under a unique `target/test-evidence/<UUID>/` namespace:

| Test | Result | Log |
| --- | --- | --- |
| `BilingualLayoutAcceptancePlaywrightTest#allTabsAndChatFitBothLanguagesAcrossDesktopTabletAndMobile` | PASS: 1 test, 0 failures/errors/skips | `target/package4-acceptance-1.log` |
| `UiCleanupDefectRegressionPlaywrightTest#progressLabelsStayReadableForApprovalAndDelegatedAtEveryTargetSize` | PASS: 1 test, 0 failures/errors/skips | `target/package4-acceptance-2.log` |
| `UiProductRefinementPlaywrightTest#captureWorkspaceScreens` | PASS: 1 test, 0 failures/errors/skips | `target/package4-acceptance-3.log` |

`TestEvidence` and its confinement test are included. Production source, CSS, assertions, request budgets and counters have no Package 4 diff. `PaginationGeometryDiagnosticTest` remains deliberately untracked and is not included in the Package 4 commit.

No further fix was made to the deferred test. Package 5 ADR/README/readiness edits and the final full regression remain pending. No claim that the complete pre-submission task is done.

Constants for later sign-off remain +/-3 calendar days and the PRD's three channels; the discarded recent-message count is not part of the enum policy. Capability-origin-not-ownership, ordinary manual same-day collapse and school-bill historical/currency matching limits are retained in PACKAGE_2.md for the pending ADR.

## Protected behavior and artifacts
Since Package 3 commit, all tracked production files are unchanged. Current uncommitted changes affect test image paths only plus the new evidence helper/confinement test/opt-in diagnostic and this report. PhaseFourService, CrossBorderService, LlmIntent/strict parser/provider adapters and financial calculation logic have no Package 4 diff. Earlier TransactionService changes are exclusively the approved receipt override namespace/reset cleanup; earlier read-only insight changes are wording-only.
