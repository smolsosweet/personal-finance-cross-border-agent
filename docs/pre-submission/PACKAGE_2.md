# Package 2 — TC07 reviewed receipt linking

Completed locally, synthetic data only. No live model/external request, push, deploy or Render mutation.

## Design and scope
- Only successful receipt-image extraction followed by explicit reviewed save is covered. Ordinary manual entry, bank-event ingest and Sandbox history retain their protected reference/fingerprint/receipt-ID dedupe.
- Expense + currency + equal BigDecimal amount + normalized merchant + confirmed calendar date within +/-3 days: one strong candidate links evidence without a financial write.
- Multiple same-amount/currency candidates within the window, different merchant, or a candidate with different confirmed evidence asks. No expense is created until the user chooses a candidate or explicitly confirms another purchase.
- Out-of-window/no-candidate receipts use existing manual ingestion. If its protected reference dedupe returns unrelated history, reject rather than attach false evidence.
- Capability: random server token, server session owner, origin hash, 20-minute expiry; cleared by reset. Fields remain untrusted and validated. This proves origin/choice, NOT financial ownership (shared unauthenticated workspace).
- Submission replay returns its saved result. Identical confirmed uploads before an override resolve to one linkage.
- Override operation is random and stored server-side only, bound to the submission/evidence. Client sends only the capability and explicit choice. Its namespaced fingerprint and reference are the sole approved dedupe exception.
- Every override evidence has confirmed hash + operation identity, its own transaction, real confirmed date (no fabricated time). A later new identical upload finds two expenses and asks again.
- Linking changes no transaction, account, budget or dashboard totals. One override adds one confirmed manual expense and 50,000 VND in the test's spending/budget. Existing manual-entry account-balance behavior is unchanged: it records expenses, not simulated bank balance events.

## Offline TC07
User approved a fixed sample, visible/available only with app.demo-tools-enabled=true. Transactions → Add transaction → Read receipt with AI → Use synthetic sample receipt → review → save. Uses the existing SEED-15 Highlands Coffee 130,000 VND expense and its recorded date, issues the same session capability, and uses the exact same save/link/override path. No model call or financial insert occurs when issuing the sample. Missing/changed seed rejects; request-supplied sample fields are ignored. Disabled demo tools returns 404 and omits the button.

A headed offline automated rehearsal also works:
mvn -Dtest=ReceiptLinkPlaywrightTest -Dtc07.headed=true test
Its synthetic extraction stub is explicitly test-only. The separate fixed-sample browser test uses no extraction stub at all.

## Schema
Additive receipt_submissions (origin/session/expiry, confirmed fields, result, private operation) and receipt_evidence (identity/hash/transaction/time). Standard VARCHAR/DECIMAL/DATE/TIMESTAMP types, H2 PostgreSQL mode tested. No existing financial columns changed. A real PostgreSQL runtime was not executed; no claim of deployed verification. Reset deletes these records before financial transaction reset.

## Actual verification
Final package gate: 46 tests, zero failures/errors/skips, BUILD SUCCESS at 2026-10-10 14:10:03 +07.
Command: mvn -Dtest=ReceiptLinkIntegrationTest,ReceiptLinkPlaywrightTest,ReceiptSampleGuardTest,ReceiptSaveBoundaryInspectionTest,TransactionServiceTest,GeminiDocumentExtractionServiceTest,GeminiMinimalContextPolicyTest test
Log: target/pre-submission-package2-gate.log.
- ReceiptLinkIntegrationTest: exact/same-confirmed receipt, two concurrent override requests + replay, third identical submission, totals/budget delta, window boundary/no-match/out-of-window, merchant ambiguity/multiple candidates/different evidence, non-expense types/currency, forged/foreign/expired/reset capability, private-operation rejection, existing reference collision, link/ask/override/reject audit.
- Existing TransactionServiceTest and explicit manual/bank/Sandbox dedupe coverage passed.
- Browser automation: VI/EN receipt notice and override, third upload asks, desktop/mobile simulated layout; screenshots under unique target/tc07-browser/<run UUID>/ paths. This is not a manual or physical-phone test.
- Offline fixed-sample button works without Gemini/extraction calls; endpoint/button guard passed.
- No full suite yet (reserved for final gate).

Intermediate authoring failures are not PASS: initial browser selector matched both document flows; initial language was not explicitly selected. Corrected the new test fixture/selectors. A new private-operation test then found reject audit echoed an untrusted input identifier; fixed reject audit to omit that identifier. The final gate above ran after all fixes.

## New visible strings for VI/EN review
| English | Vietnamese |
|---|---|
| Receipt review | Xem lại biên lai |
| Linked expense | Khoản chi đã liên kết |
| Link this expense | Liên kết khoản chi này |
| This is a different purchase | Đây là khoản chi khác |
| Receipt linked to the existing expense. No new expense was created. | Biên lai đã liên kết với khoản chi hiện có. Không tạo khoản chi mới. |
| Similar expenses found. Choose the matching expense or confirm a different purchase. Nothing was added. | Có khoản chi tương tự. Chọn khoản chi phù hợp hoặc xác nhận lần mua khác. Chưa ghi thêm khoản chi. |
| A different purchase was recorded once and linked to this receipt. | Đã ghi một khoản chi cho lần mua khác và liên kết với biên lai này. |
| Receipt saved and linked to the recorded expense. | Đã lưu biên lai và liên kết với khoản chi được ghi nhận. |
| Receipt request rejected. Reopen and review the receipt. No financial data changed. | Yêu cầu biên lai bị từ chối. Hãy mở và xem lại biên lai. Không thay đổi dữ liệu tài chính. |
| Receipt origin unavailable. Read the image again. | Chưa xác nhận được nguồn biên lai. Hãy đọc lại ảnh. |
| Record one additional expense for this different purchase? | Ghi thêm một khoản chi cho lần mua khác này? |
| Use synthetic sample receipt | Dùng biên lai mẫu mô phỏng |
| Sample receipt details. Review before saving. | Thông tin biên lai mẫu. Hãy kiểm tra trước khi lưu. |
| Synthetic sample receipt. Review and edit before saving; no Gemini request. | Biên lai mẫu mô phỏng. Hãy xem và sửa trước khi lưu; không gọi Gemini. |
| Synthetic sample unavailable. Use a local seeded workspace. | Biên lai mẫu chưa sẵn có. Hãy dùng workspace local có dữ liệu seed. |

Constant for team sign-off: ReceiptLinkService.DEFAULT_DATE_WINDOW_DAYS=3, configurable finbridge.receipt.date-window-days (0–31). School-bill historical/cross-currency matching and normal manual identical same-day collapse remain roadmap/known limits, not silently changed.

Protected payment/approval/Policy Guard/FX/recipient matching/LLM contract remain untouched. TransactionService diff is only additive reset cleanup, shared manual helper and the explicitly approved override reference/fingerprint namespace.