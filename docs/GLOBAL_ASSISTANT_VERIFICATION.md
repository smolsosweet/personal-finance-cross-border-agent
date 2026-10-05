# Nghiệm thu Global Assistant Workflow

Ngày 05/10/2026 (UTC+7). Baseline: 6096bbf.
Chỉ local, synthetic và Payment Sandbox. Không push, deploy hoặc thao tác Render.

## 1. Khả năng và tiêu chí nghiệm thu

Contract đầy đủ: [GLOBAL_ASSISTANT_WORKFLOW.md](GLOBAL_ASSISTANT_WORKFLOW.md).

| Khả năng | Nguồn và giới hạn | Tự động/mock | Gemini thật |
|---|---|---|---|
| Số dư hiện tại, một hoặc tất cả tài khoản | payment_source_accounts đã kết nối/xác minh + sandbox_accounts; tổng riêng từng currency | Đạt | Đạt |
| Chi tiêu / ngân sách | Kỳ demo hiện tại; ngân sách không phải account balance | Đạt | Đạt |
| So sánh / lý do kênh | Bill xác minh, quote mô phỏng; Bank B không thực thi | Đạt | Đạt |
| Dự báo trước thanh toán / runway | Bill chưa trả; quote còn hạn; monthly form được xác nhận | Đạt | Đạt |
| Trạng thái plan / thanh toán | ID và trạng thái backend, choice khi mơ hồ | Đạt | Đạt |
| Số dư lịch sử sau thanh toán | Receipt bất biến, transaction ID/thời điểm; không gắn nhãn current | Đạt | Đạt |
| Draft theo phiên chat | Bill/version + account + channel/quote đã xác minh; explicit request | Đạt A/B ở backend và browser | Đạt |

## 2. File và routing/context thay đổi

- GlobalAssistantQueries.java: catalogue chỉ đọc, tổng riêng từng currency; loại trừ planner, người nhận và tài khoản phí.
- LlmIntent.java, LlmIntentContract.java, ModelConversationContext.java: thêm current/history intents/topics. Contract vẫn đúng bốn trường; parser strict không đổi.
- FinanceChatService.java: không dùng surplus làm account balance; lookup độc lập không bị FX context cũ chặn; draft vẫn chịu stale-response/context checks. Giữ routing explicit surplus cũ.
- SessionConversationService.java: scope current/history/prospective riêng; ưu tiên tham chiếu rõ; token backend theo phiên/revision/expiry. Draft dùng các đối tượng phiên, không sửa lựa chọn chung.
- PersonalFinanceInsights.java: projection nhận đối tượng đã resolve, dùng phép tính cũ; historical balance có thời điểm giao dịch.
- PhaseFourService.java: assistant dispatch/fallback và entry point draft chat, dùng chung builder. Entry point màn hình vẫn kiểm tra bill đang chọn.
- PhaseOneController.java, home.html, app.js, ConversationPresentation.java: review theo plan, banner theo trạng thái thực, current/history/clarification.
- Test mới: GlobalAssistantCorpus, GlobalAssistantFixtures, GlobalAssistantIntegrationTest, GlobalAssistantPlaywrightTest, GlobalAssistantGeminiLiveIT. Cập nhật test context, strict contract và hosting test.
- HostingDemoPlaywrightTest dùng RANDOM_PORT thay vì cổng cố định 8124 để không xung đột app live local.

## 3. Lệnh test và kết quả thực tế

    mvn "-Dtest=GlobalAssistantIntegrationTest,GlobalAssistantPlaywrightTest,SessionConversationIntegrationTest,RunwayIntegrationTest,StrictLlmIntentParserTest" test

Lượt đầu phạm vi B: 77 test, 2 fail và 1 error. Test giả định MoMo unavailable không đúng seed; browser cố đóng panel đã đóng khi review; đổi fragment URL không tải lại fixture banner. Đã sửa test/fixture, không nới policy.

    mvn "-Dtest=GlobalAssistantIntegrationTest,GlobalAssistantPlaywrightTest,SessionConversationIntegrationTest" test

Lượt sau: 54/54 đạt. Lượt cuối sau thêm explicit bill override: **55/55 đạt**, 0 fail/error/skipped.
Các lượt này mock model. Chrome/Playwright chạy app và H2 local riêng.
Corpus cố định 16 câu Anh/Việt chạy ở bốn tab (64 lượt UI), không chứng minh chất lượng Gemini.

    mvn test *> target/global-assistant-full-suite.log

Lượt full đầu: 305 test, 1 fail và 2 error. Fail do explicit surplus cũ bị routing bỏ qua; 2 error do test hosting và app Gemini cùng cổng 8124.

    mvn "-Dtest=PhaseFourIntegrationTest,HostingDemoPlaywrightTest" test

Sau sửa đúng hai nguyên nhân: **19/19 đạt**, 0 fail/error/skipped.
    mvn test *> target/global-assistant-full-suite-final.log

Full suite bản cuối: **305/305 đạt**, 0 fail/error/skipped, BUILD SUCCESS,
03:18 phút; kết thúc 08:33:47 ngày 05/10/2026 UTC+7.
Không chạy lại full suite sau từng sửa nhỏ; lượt đầu fail nên cần một lượt cuối
để xác minh đúng hai regression đã xử lý. Test default không gọi live API.

    mvn "-Dtest=GlobalAssistantGeminiLiveIT" "-Dglobal.live.url=http://localhost:8124" test

Opt-in local Gemini, app PID 4516, profile gemini, model gemini-3.5-flash-lite.
Database riêng: target/global-assistant-live. App ban đầu được chạy nhầm database gemini-live-check; không chạy mutation vào database đó.
Lượt live đầu: provider GEMINI_SERVICE_UNAVAILABLE, không có intent hợp lệ, 0 plan/0 payment.
Cho phép đúng một lần thử lại; counter giữ nguyên, tính cả failure vào cap 20.
Sau đó 13 câu model đạt; quote cấp lúc startup hết hạn ngay trước phê duyệt.
UI vô hiệu nút approve theo timer, server reload ẩn nút và báo FX QUOTE EXPIRED.
Đây là safety block đúng, không kéo dài quote để test qua.

    mvn "-Dtest=GlobalAssistantGeminiLiveIT" "-Dglobal.live.url=http://localhost:8124" "-Dglobal.live.resume=true" test

Tiếp tục checkpoint, không gửi lại 13 câu đã đạt: làm mới quote bằng UI,
review draft thay thế rồi phê duyệt riêng; chạy năm câu còn thiếu.
Lượt tiếp tục đầu dừng ở assertion UI: test yêu cầu nút disabled nhưng server đã ẩn nút.
Sửa assertion để xác minh không có executable action và đúng backend reason;
lượt đó không dùng thêm generation. Lượt tiếp tục cuối **1/1 test đạt**.

Tổng **19 generation attempts/20**, gồm 18 kết quả model hợp lệ và một provider failure.
18/18 kết quả được phân loại đúng intent mong đợi, qua strict schema, có evidence số liệu.
Injection riêng bị prefilter chặn, dùng 0 generation. Không thử quota exhaustion.
Độ trễ UI quan sát 1.201–9.767 ms (tức 1,201–9,767 giây):
hai câu đầu 9,767 và 8,983 giây, 16 câu sau 1,201–1,419 giây.
Không coi đây là đo latency thuần của API hoặc cam kết SLA.

Evidence chi tiết, gồm câu hỏi/intent/schema/latency/actual reply/plan/payment count:
[GLOBAL_ASSISTANT_LIVE_EVIDENCE.json](GLOBAL_ASSISTANT_LIVE_EVIDENCE.json).
Ảnh cuối: [GLOBAL_ASSISTANT_LIVE_FINAL.png](GLOBAL_ASSISTANT_LIVE_FINAL.png).

| Ca live tiêu biểu | Kết quả quan sát |
|---|---|
| Vietcombank current EN/VI | 82.000.000 VND; không trừ buffer |
| All connected balances | 314.000.000 VND trước khi thêm fixture Bank A thứ hai |
| Bank A mơ hồ | Hai choices trong chat; chọn Everyday; follow-up trả 100.000.000 VND |
| VCB tuition projection | 11.153.028 VND, ESTIMATE, chưa thanh toán |
| Bank A runway + form 8 triệu/tháng | 3,27 tháng, chỉ đọc |
| Draft A | ACT-CE8D3D08-EE6 AWAITING_APPROVAL; 0 payment |
| Quote hết hạn | Approve không thực thi; refresh tạo replacement, draft cũ INVALIDATED |
| Receipt A / current sau A | SBOX-DD713225-F30; 29.239.200 VND; quote hết hạn không chặn |
| Hỏi nếu đóng bill đã trả | Trả receipt, nói không trừ học phí lần nữa |
| Draft B từ chat | ACT-5209CEF0-067; source VCB_VND; review TUITION-B-DEMO; workspace vẫn A/PAYER_VND |
| Phê duyệt B | B COMPLETED, tổng hai payments A và B; shared selections không đổi |
| Injection | Prefilter BLOCKED; snapshot tài chính không đổi |

## 4. Bằng chứng bất biến dữ liệu

Test snapshot tất cả public tables trước/sau từng câu hỏi chỉ đọc, trừ audit_log và conversation_messages được phép ghi.
Bao gồm shared bill/account selections.

- Receipt sau học phí Sandbox Bank A: 29.239.200 VND.
- Sau một giao dịch Sandbox tiếp theo 500.000 VND: current Bank A 28.739.200 VND; receipt học phí giữ nguyên 29.239.200 VND.
- Quote hết hạn không chặn current/history; bill đã trả không bị trừ lần hai.
- Hai phiên chọn hai tài khoản riêng; choice token không dùng được ở phiên khác.
- Workspace chọn A; chat tạo B/VCB; review B; trước duyệt 0 receipt và số dư không đổi. Duyệt B và gửi lại approval chỉ có một giao dịch. A vẫn chưa trả và vẫn được chọn.
- Bill B đổi vô hiệu draft; expiry chặn approval; source/channel mismatch và version sai bị từ chối. Không tự thay bằng A hoặc kênh khác.
- Bill B chưa trả không dùng nhầm receipt hay trạng thái đã trả của A.

## 5. Bằng chứng code tài chính được bảo vệ

So sánh 6096bbf, chuẩn hóa LF: PhaseFourService.java từ dòng
PaymentSourceAccount source=paymentSource(sourceAccountId); trong builder đến EOF **giống hoàn toàn**.
SHA-256: 8F481A6FCD546089BEC419DB0FA8E8F4FD64002155371BF21D4F786EB4F5A452.
Bao gồm financial plan fields/snapshot, Policy Guard, approve/execute, idempotency, balance/ledger writes và receipts.

Diff rỗng cho CrossBorderService, LivingExpenseRunwayService, StrictLlmIntentParser, Gemini/OpenAI/Ollama provider implementations, schema, pom và requirements.
Selected-bill precondition khác nhau theo entry point là phạm vi B được người dùng cho phép. Entry point workspace vẫn giữ điều kiện selected; chat phải có verified explicit references. Active/unpaid/version và toàn bộ policy không đổi.

## 6. Blocker và Definition of Done

Gemini/browser và full suite đã đủ evidence cho corpus này. Không còn blocker nghiệm thu.
Commit local được cho phép khi các gate đạt, với title:
feat(assistant): complete global financial conversation workflow.
Không push/deploy. Commit SHA nằm trong Git; báo cáo được lưu cùng commit.
Demo vẫn shared synthetic, không authentication/tách dữ liệu production.
Planner chưa ánh xạ Sandbox; projection giữ disclaimer. Không thêm ngân hàng thật, OCR, training hoặc tool calling.

Known issue quan sát: một GEMINI_SERVICE_UNAVAILABLE ban đầu, chưa xác định là HTTP 5xx
hay transport error vì provider chủ động dùng thông báo an toàn. Fallback không tạo plan/payment.
Hai model calls đầu chậm hơn các call sau. Cần giữ timeout/fallback hiện có.
Lượt live cần tiếp tục sau expiry; không tuyên bố toàn bộ chạy liền mạch ngay lần đầu.

**Definition of Done: đạt cho Global Assistant Workflow và phạm vi B đã chốt.**
Không đồng nghĩa production ready, không chứng minh chống mọi prompt injection
hoặc Gemini luôn khả dụng. Dừng ở phase này.
