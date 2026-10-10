# Recipient-verification label acceptance

Date: 2026-10-10. Base: `9bdf2ad`. Local-only wording task; no push/deploy.

## 1. Inspection before edits

[Complete hit table](INSPECTION.md): **561 matching lines** from case-insensitive repository search, with file:line, original text, location and type. Generated target files and agent/Git metadata are excluded. Existing untracked review documents were inspected but preserved; no tracked slides were found.

## 2. Final proposed wording

| Use | English | Vietnamese |
|---|---|---|
| Positive badge, filter option, beneficiary card, recipient audit status | Matched against simulated School Registry | Khớp Danh bạ Nhà trường mô phỏng |
| Source heading and recipient audit actor display | Simulated School Registry | Danh bạ Nhà trường mô phỏng |
| Step 2 | Check simulated registry match | Đối chiếu danh bạ mô phỏng |
| Negative beneficiary state | Beneficiary mismatch | Người thụ hưởng không khớp |
| Negative filter/badge (existing meaning preserved) | Needs verification | Cần xác minh |

Bill/chat/notification sentences use the same registry terminology while retaining amounts, references, state and approval requirements. No dedicated last-checked time is recorded by `CrossBorderService.verifyRecipient`, so none is displayed. Bill update time, quote time, receipt time and genuine audit-event time retain their original meanings.

## 3. Files changed and implementation boundaries

[One-line file change table](FILES_CHANGED.md).

`RecipientMatchPresentation` only maps display text. `ConversationPresentation` parses the original financial response and maps its assistant message at the final render boundary; user messages remain verbatim. Thymeleaf uses the adapter for registry reasons, flash notices, payment explanations, notifications, clarification, audit details and the legacy demo chat. Audit status/actor labels are presentation mappings; raw records, event codes, reason codes and data-status remain intact. Notification identities/versions are unchanged.

Current README/payment/global-assistant guides clarify the simulation. Historical Markdown reports receive a scope note; their recorded test outcomes, raw JSON evidence and prior screenshots are not rewritten. The separate untracked team-review documents are not included or edited.

## 4. Automated tests and browser evidence

Initial targeted attempts identified a newly introduced template binding error and incorrect new test fixtures/selectors; these were corrected without changing protected services. The pre-final focused run completed **22/22** checks. A first broad test selector also picked up cloud `*IT` classes; `DeployedAcceptanceIT` stopped at its missing-URL setup assertion before browser/network access. Subsequent full runs use Maven's default class-name patterns.

Full-suite diagnosis also identified two stale test interactions: a direct click on the already-hidden channel picker, and an ambiguous `[role=status]` selector. They now click the actual card and target the flash notice; financial/text expectations were preserved. One existing layout capture was blocked by a Windows-mapped PNG file. The layout test now accepts an evidence-directory property, preserving its default path and all assertions.

| Run | Command (Maven 3.9.16, Java 21) | Actual result |
|---|---|---|
| Focused checks | `mvn -Dtest=RecipientMatchPresentationTest,RecipientMatchLabelsPlaywrightTest,FinBridgePlaywrightE2ETest,BusinessUxPlaywrightTest test` | 22 tests, 0 failures/errors/skips; BUILD SUCCESS. |
| Full standard suite including Playwright | `mvn '-Dtest=Test*,*Test,*Tests,*TestCase,!ScreenshotEvidenceCollectorTest' test` with `FINBRIDGE_GEMINI_LIVE_SMOKE=false` | 385 tests: 0 assertion failures, 1 screenshot file-write error, 1 live-provider test skipped. BUILD FAILURE due solely to the Windows file lock. |
| Affected layout class retry | `mvn -Dtest=BilingualLayoutAcceptancePlaywrightTest -Dfinbridge.layout.evidence.dir=docs/recipient-labels/layout-regression test` | 3 tests, 0 failures/errors/skips; BUILD SUCCESS. All layout assertions executed using a fresh output directory. |

**Consolidated: 384 distinct tests executed and passed; 1 live Gemini smoke test skipped.** This is not a claim that the full-suite invocation itself succeeded. See [machine-readable results by class](TEST_RESULTS.json). The standard suite excludes optional cloud/live `*IT` classes; the unrelated, pre-existing untracked `ScreenshotEvidenceCollectorTest` was excluded to preserve its review artifacts. No tests or assertions were removed. Run logs remain in `target/recipient-labels-targeted-final.log`, `target/recipient-labels-full-suite-final.log`, and `target/recipient-labels-layout-retry.log`; the results and screenshots in this folder survive `mvn clean`.

All acceptance actions used local synthetic H2 and Payment Sandbox. Chat intent was mocked; **zero live Gemini calls and no Render requests**. The skipped live test is not reported as PASS. No unresolved automated or browser blocker remains for this display-only task.

The new browser checks cover bill badge/filter/source, negative mismatch, draft review, final approval dialog, completed receipt/audit and chat status/comparison in EN/VI. Bill layouts include 390px and 1440px. This is browser automation, not manual phone testing. See [evidence](evidence/). Registry audit sentences are translated as complete known templates in Vietnamese while institution names, bank codes and account values remain verbatim.

## 5. Protected files / values

The diff check against HEAD covers all tracked backend files except the display-only `ConversationPresentation`, all resource/config/schema files except UI assets/templates, plus pom, Docker and hosting configuration. No protected implementation/config diff. The new display adapter introduces no verification decision, financial calculation, provider call or timestamp generation. Browser assertions additionally verify stored audit `VERIFIED` and original registry evidence remain unchanged.

## 6. Remaining search hits and wording review

[Complete post-change hit table](REMAINING_HITS.md).

- Internal `VERIFIED`, `MISMATCH`, `RECIPIENT_VERIFICATION`, schema columns, method names and reason codes remain by contract; they are not renamed.
- Protected backend templates retain legacy phrases; the display adapter maps recipient/bill claims on the UI. Unit/integration assertions on those raw strings still test their unchanged contract.
- Source-account eligibility uses a different internal verification concept; this task does not relabel it as a school-registry match. Generic test/build verification and untrusted-input terms are unrelated.
- AI extraction warnings explicitly say extraction does not verify the recipient; they remain honest and unchanged.
- Historical JSON/screenshots and pre-edit inventory retain original text as evidence. Historical Markdown scope notes distinguish their terminology from current UI.
- Team wording review: approve the EN/VI positive label and source wording above. They are proposed copy implemented locally; no external school, bank or partner verification is claimed.
- Real-phone verification, external-bank validation, RAG and cloud deployment are outside this task.
