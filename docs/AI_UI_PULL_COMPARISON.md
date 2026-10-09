# Đối chiếu main AI và UI local — 09/10/2026

Đã pull main từ 2d161f9 lên 3f68b76070bd010361ad8cfc7e8aaa8b93ab1fc4 theo fast-forward. Local hiện gồm code AI mới cộng các sửa UI D1–D3 chưa commit. Không push hoặc deploy. Bản UI trước pull còn trong stash `local-ui-D1-D2-D3-before-AI-pull-2026-10-09`.

## Thay đổi và ảnh hưởng

| Phần | Thay đổi trên main | Ảnh hưởng |
|---|---|---|
| Upload ảnh giao dịch/hóa đơn | Thêm /api/ai/extract-document và GeminiDocumentExtractionService; điền form để người dùng xem lại/lưu | Giữ lại cùng D1–D3. Chưa gọi Gemini thật trong đợt này. |
| Chi tiêu | Thêm truy vấn theo ngày/danh mục, pending review và rule routing trước model | Có hồi quy phân biệt chi tiêu và số dư sau học phí, xem bên dưới. |
| Nhập giao dịch | Thêm form/endpoint /transactions/manual, currency, category, mô tả, phương thức thanh toán | Có thay đổi backend TransactionService. Nguồn hiện mặc định CHECKING; paymentMethod chỉ nằm trong mô tả (TransactionService:201–232). Chưa chứng minh chọn đúng nhiều nguồn tiền/tiền mặt. |
| Provider/khởi chạy | Dùng Gemini, bỏ adapter Ollama/OpenAI và start-local.cmd | Không dùng launcher CMD cũ. Có thể gọi start-local.ps1 qua powershell từ CMD; startup có cơ chế nhập/lưu key mã hóa theo tài khoản Windows. |
| CSS/JS/template | Upload AI, đóng form runway, nhãn/badge giao dịch mới | app.js tự ghép. Hai xung đột textual ở phần thêm cuối app.css và URL phiên bản app.js; đã giữ cả hai bên. Asset URL local dùng 20261009-ai-ui-local. |
| Chính sách/thanh toán | PhaseFourService, schema.sql, pom.xml không đổi giữa hai commit | Chưa thấy thay đổi trực tiếp guard/executor; test UI approval/delegated/receipt vẫn đạt. Đây không phải audit toàn bộ safety. |

## Test đã thực sự chạy

```powershell
& 'C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd' '-Dtest=UiCleanupDefectRegressionPlaywrightTest,UiCleanupPlaywrightTest,GeminiDocumentExtractionServiceTest,LlmProviderConfigurationTest,PersonalFinanceAiIntegrationTest' test
```

Kết quả: **27 test, 22 PASS, 5 FAIL, 0 error/skipped — BUILD FAILURE**.

| Nhóm | Kết quả |
|---|---|
| UiCleanupDefectRegressionPlaywrightTest | 3/3 PASS |
| UiCleanupPlaywrightTest | 5/5 PASS |
| GeminiDocumentExtractionServiceTest | 4/4 PASS (HTTP stub, không Gemini thật) |
| LlmProviderConfigurationTest | 3/3 PASS |
| PersonalFinanceAiIntegrationTest | 7/12 PASS, 5 FAIL |

Browser automation dùng Chrome headless, database H2 riêng; không kiểm tra điện thoại thật/Render. Các screenshot cũ bị test ghi lại đã được khôi phục; ảnh mới lưu trong ai-ui-pull và ui-ux-defect-fixes/after.

Năm ca không đạt:

- exactAffordabilityUsesEligibleSelectedSourceFeeAndSafetyBufferWithoutMutation
- completedTuitionUsesImmutableReceiptBalanceInsteadOfAnExpiredQuoteProjection
- englishAndVietnameseStubbedClassificationUsesStrictFourFieldContractAndDoesNotMutate
- ambiguityConfidenceOtherPeriodAndCustomScopeRequireClarification
- malformedProviderFailureAndInjectionLeaveAllFinancialStateUnchanged

Ba ca cuối liên quan kỳ vọng cũ về gọi model/confidence/fallback trong khi rule mới trả trực tiếp dữ liệu chi tiêu. Cần đối chiếu phạm vi mới trước khi điều chỉnh tests; không kết luận tất cả là lỗi sản phẩm. Ca provider-failure dừng tại assertion đầu nên lượt này không chứng minh các assertion injection phía sau đạt.

## Hồi quy xác nhận — chưa sửa

**Mức độ: high, trả sai loại thông tin tài chính, không quan sát thấy tự chuyển tiền.**

Reproduction với model mock và synthetic seed:

1. Hỏi `Can I afford living costs after tuition?`.
2. Tạo và duyệt kế hoạch BANK_A trong database test riêng.
3. Hỏi `How much remained after I paid tuition?`.

Expected: bước1 trả dự báo sau học phí; bước3 trả số dư lịch sử receipt 29.239.200 VND.

Actual:

```text
ROUTING_BEFORE_PAYMENT
Reporting period: 2026-10-01 – 2026-10-31.
No recorded spending for Education in this period.

ROUTING_AFTER_PAYMENT
Reporting period: 2026-10-01 – 2026-10-31.
Education: 70760800.00 VND.
Total for requested categories: 70760800.00 VND.
Source: recorded expenses that are automatic or confirmed; pending-review transactions are excluded.

MODEL_CLASSIFY_INVOCATIONS=0
```

Nguyên nhân: PersonalFinanceInsights.isSupportedSpendingScope (dòng53) kết hợp alias tuition/Education với từ costs/paid/pay; FinanceChatService (dòng73–75) chọn EXPLAIN_SPENDING_SUMMARY trước khi gọi model. Do đó câu hỏi dự báo/receipt bị chuyển sang tổng chi tiêu. Liên quan yêu cầu Global Assistant phân biệt chưa thanh toán/đã thanh toán/số dư hiện tại.

Đã chạy test chẩn đoán tạm `PulledAiRoutingDiagnosticTest#observeTuitionRoutingWithoutChangingFinancialState`: **1/1 PASS** cho assertions số dư/receipt không bị thay đổi bởi câu hỏi; output xác nhận routing sai. PASS chẩn đoán không phải PASS yêu cầu học phí. Fixture tạo đúng một thanh toán Sandbox được duyệt rõ ràng. File test tạm đã được gỡ, không sửa production backend. [Log](ai-ui-pull/ai-ui-routing-diagnostic.log).

[Log 27 test](ai-ui-pull/ai-ui-pull-verification.log).

## Cách xem local bằng CMD

```bat
cd /d D:\personal-finance-cross-border-agent
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-local.ps1 -Port 8124
```

Launcher có thể hỏi Gemini key nếu không có biến môi trường/key lưu mã hóa. Nhập trực tiếp trong terminal, không gửi key vào chat. Startup không chứng minh Gemini đã hoạt động; lượt kiểm chứng này không gọi model thật.

## Kết luận

Pull và ghép UI local hoàn thành, không còn conflict marker/index conflict. D1–D3 tương thích với UI mới trong 8 test browser đã chạy. **Chưa thể chốt AI/finance tương thích hoàn toàn** vì hai hồi quy học phí được xác nhận và ba kỳ vọng test cần rà. Không sửa lỗi nghiệp vụ, không commit/push/deploy.
