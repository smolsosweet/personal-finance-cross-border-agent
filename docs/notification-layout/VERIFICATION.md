# Notification priorities and bilingual layout verification

Date: 2026-10-10. Base commit: `edbbdff`.

## Scope

Completed the pending notification-priority and layout changes. Verification used local H2 databases dedicated to each test class, synthetic accounts/bills/events and Payment Sandbox. No Render reset, approval, stop, restart or hosting change was performed. No live model generation was requested.

## Notification rules

| Situation | Priority | Meaning |
| --- | --- | --- |
| Verified unpaid bill: more than 7 days until the recommended eligible channel's latest safe payment date | INFO / UPDATE | Prepare when ready; no urgent approval reminder |
| 3–7 days until that safe date | MEDIUM / ACTION | Prepare a payment plan |
| 2 days or less, including a passed safe date | HIGH / ACTION | Time-sensitive payment attention |
| Beneficiary mismatch or no eligible route | BLOCKED / ACTION | Resolve the specific blocker before payment |
| Existing pending plan | Its policy result and safe-date urgency | Link to that exact plan instead of asking to create another |
| Completed payment | INFO / UPDATE | Exact immutable receipt; no further approval reminder |
| Routine category/purpose review | MEDIUM / ACTION | Review remains required after marking the notification seen |
| Highest-used monthly budget below 80% | No budget notification | Avoid low-usage noise |
| Budget usage 80% to below 100% | MEDIUM / UPDATE | Early warning |
| Budget usage at or above 100% | HIGH / ACTION | Review the budget; actual percentage can exceed 100% |
| Primary checking balance at or above the existing 3 million VND planning buffer | No buffer notification | Avoid repetitive healthy-balance suggestions |
| Primary checking balance below that buffer | HIGH / ACTION | Show the shortfall and link to accounts |
| Upcoming reserved money | INFO / UPDATE | Planning information; no payment is made |

Budget thresholds and ranking use exact recorded amounts, not the rounded/capped progress-bar percentage. Notifications retain existing chronological ordering and browser-local seen state. Marking seen does not approve, pay, resolve, or remove required actions. Priority is recalculated when the workspace is rendered/refreshed; this is not a background push-notification scheduler.

Relevant implementation: `CrossBorderService.tuitionInsight`, `PhaseOneController.home`, `TransactionService.proactiveFeed`, `FinanceWorkspaceService.attentionItems`.

## Layout changes

- Comparison cards share a footer structure with consistent action, guidance, quote-status and refresh positions. Supported and unsupported routes align their status rows.
- More readable invoice/channel text; explanatory guidance no longer inherits the small uppercase fact-label style.
- A single visible bill uses the available width, with compact desktop columns and a stacked mobile layout. Multiple bills retain the horizontal carousel.
- Hide one-page invoice pagination and horizontal-scroll hints/controls when there is no overflow.
- Keep invoice count and pagination together in a compact footer.
- Remove the duplicate payment-example tools from Overview; existing payment tools remain in Payments & History.
- Update static-asset versions so the new CSS/JavaScript can replace cached versions.

## Focused automated results

The final results below consolidate focused runs; they are not a claim that a full suite was rerun. Initial failures caused by incorrect new test fixtures/selectors/label expectations were corrected and the affected checks rerun.

| Test class | Distinct checks passed | Coverage |
| --- | ---: | --- |
| PhaseThreeIntegrationTest | 12 | Bill verification, channels, safe-date urgency, plan-specific notification |
| PhaseTwoIntegrationTest | 11 | Categorization, review/Undo, internal-transfer accounting and feed contract |
| NotificationRiskIntegrationTest | 3 | Exact 80%/100% boundaries, buffer boundary, healthy-seed suppression, no financial mutation |
| NotificationCenterPlaywrightTest | 12 | VI/EN notification panel, seen/filter behavior, chronological order, exact completed receipt, two-day urgency translation |
| StudentExpensePlaywrightE2ETest | 13 | Bill selection/lifecycle, filters/pagination, channel selection/eligibility, asynchronous updates and alignment |
| WorkspaceListScrollPlaywrightTest | 5 | Real wheel/button scroll, short-list controls, account alignment, five-row history pagination and compact overview |
| BilingualLayoutAcceptancePlaywrightTest | 3 | All tabs and overview subviews; chat; fresh/expired/unsupported channels; detail dialogs |
| **Total distinct relevant checks** | **59** | **Focused verification only** |

Browser automation used installed Chrome headless and simulated viewports 360, 390, 768 and 1440px. Notification tests additionally cover 1366 and 1920px. Assertions check page containment, equal comparison action positions, status alignment, dialog bounds and absence of JavaScript errors. Screenshots of both languages are saved in [evidence](evidence/).

The expiry-layout scenario advances the expiry timestamp in the browser to exercise rendering; it does not claim a live provider failure or an actual expired approval was executed. Existing bill/channel regression checks remain supporting workflow evidence.

## Limits and final user checks

- This is browser automation, not manual testing or a physical phone test.
- No new Render acceptance or Gemini availability test is claimed.
- Financial execution, quote calculation, Policy Guard, approval, idempotency, receipt storage, schema and dependencies were not changed by this task.
- Before presenting the deployed build, confirm its Live commit, reload cached assets, and check chat with the keyboard open on a real phone. Avoid shared-workspace reset/payment without coordinating with the team.
- The shared synthetic environment remains a demo; a layout pass does not establish production readiness for real financial data.
