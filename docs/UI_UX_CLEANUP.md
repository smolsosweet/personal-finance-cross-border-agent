# FinBridge — UI/UX cleanup, 05/10/2026

## 1. Audit Step 0

Audit đã báo trước khi sửa; người dùng xác nhận “có”. Phạm vi chỉ gồm trình bày, không thay đổi nghiệp vụ hay quyết định tài chính.

| Màn hình | Mục đích | Điểm audit đã được duyệt |
| --- | --- | --- |
| Tổng quan | Số dư, chi tiêu, planner, ngân sách | Chuẩn hóa màu, typography và mẫu khối |
| Giao dịch | Lịch sử, phân loại, xác nhận/Undo | Lịch sử vừa phân trang vừa cuộn trong khối; cần bỏ cuộn dọc bên trong, giữ 5/trang |
| Tài chính du học | Hóa đơn, người nhận, so sánh kênh | Dùng chung mẫu badge, focus và tiến trình thao tác |
| Thanh toán & lịch sử | Review, phê duyệt, receipt, audit | Mode bị giấu trong công cụ demo; mã chặn chưa rõ; frontend diễn giải lại câu backend |
| Trợ lý toàn cục | Hỏi xuyên tab, context, xem plan/receipt | Nhãn mô phỏng bị panel mobile che; cần hiện trong chat |

Đã có và giữ nguyên: review khóa số tiền, tài khoản, người nhận, kênh; xác nhận trước approval; loading chat/payment; không tự gửi lại payment khi mất response; quote expiry/recovery; focus và mobile dialog.

Các vấn đề khác đã chốt: mobile ẩn ACTIVE/PAUSED; thao tác POST thường chỉ disable nút mà thiếu tiến trình; CSS chat inline cũ; điều hướng native dùng `aria-selected`; composer demo thiếu label. Không có điểm audit nào cần sửa protected backend.

## 2. File thay đổi

### View production

| File | Thay đổi và lý do |
| --- | --- |
| `src/main/resources/templates/home.html` | Mode ở header/chat, quyền hạn của plan ở review; cảnh báo dừng toàn workspace; nhãn mô phỏng trong chat; mã chặn và giải thích backend; badge audit/history; label input, `aria-pressed`, asset version mới |
| `src/main/resources/static/app.js` | Dịch đúng câu backend, giữ câu không có bản dịch; nhãn trạng thái dễ hiểu; tiến trình POST; bỏ bounded scroll lịch sử; đồng bộ semantics điều hướng |
| `src/main/resources/static/app.css` | Token màu trạng thái dùng chung; badge/button/focus/loading; audit dễ đọc hơn; header mobile; bỏ cuộn dọc lịch sử và CSS chat inline không dùng |

`fragments/conversation.html` không thay đổi. Không đổi Java production, controller, Policy Guard, Payment Sandbox, LLM parser/client, audit logic, config, dependency, schema hay requirements.

Header hiển thị mode workspace. Review đọc `latestAction.requiredPermission`, vì vậy workspace Delegated nhưng học phí vẫn Approval. Đây là hiển thị dữ liệu có sẵn, không đổi policy.

### Test và bằng chứng

- Mới: `src/test/java/com/example/finance/UiCleanupPlaywrightTest.java`, 5 test.
- Assertion điều hướng cập nhật trong `AssistantPanelPlaywrightTest`, `DeployedAcceptanceIT`, `FinanceOverviewPlaywrightE2ETest`, `PaymentWorkflowPlaywrightE2ETest`, `StudentExpensePlaywrightE2ETest`, `UiProductRefinementPlaywrightTest`.
- Kỳ vọng lịch sử chỉ phân trang cập nhật trong `UnifiedAccountsPlaywrightTest`, `WorkspaceListScrollPlaywrightTest`.
- `DeployedAcceptanceIT` chỉ cập nhật selector; **không chạy Render acceptance trong task này**.
- Ảnh mới: `docs/ui-ux-cleanup/mobile-review-vi.png`, `mobile-paused-vi.png`, `desktop-receipt-audit-en.png`.
- Team-review và screenshot collector có sẵn từ công việc khác không đưa vào commit này.

## 3. Kết quả kiểm chứng

### Tự động

Chrome qua Java Playwright, H2 riêng trong bộ nhớ, synthetic seed và Payment Sandbox. Các test AI dùng mock hoặc LLM mặc định tắt. Không kiểm chứng Gemini/Ollama thật, không chủ động gọi model thật trong task UI này. Không chạy full backend/provider suite không liên quan.

Lệnh thực thi tại repository trong PowerShell:

```powershell
$maven = 'C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd'
& $maven '-Dtest=UiCleanupPlaywrightTest,WorkspaceListScrollPlaywrightTest,UnifiedAccountsPlaywrightTest' test
& $maven '-Dtest=UiCleanupPlaywrightTest,WorkspaceListScrollPlaywrightTest,UnifiedAccountsPlaywrightTest,UiProductRefinementPlaywrightTest,UiReviewFollowupPlaywrightTest,PaymentWorkflowPlaywrightE2ETest,AssistantPanelPlaywrightTest,FinanceOverviewPlaywrightE2ETest,StudentExpensePlaywrightE2ETest,FinBridgePlaywrightE2ETest' test
& $maven '-Dtest=UiCleanupPlaywrightTest' test
& $maven '-Dtest=UiCleanupPlaywrightTest,UiProductRefinementPlaywrightTest' test
& $maven '-Dtest=UiProductRefinementPlaywrightTest#captureWorkspaceScreens' test
```

Output được chuyển vào các log `target/ux-cleanup-*.log`; báo cáo này lưu bền vững ngoài target.

| Lượt | Kết quả thực tế |
| --- | --- |
| Đầu tiên | 12 test: 10 đạt, 2 fail. Test mới kỳ vọng AGENT_PAUSED thay vì AGENT PAUSED; một test tiếng Việt bị đọc sai encoding khi chỉnh file. Đã sửa test theo source thật; không sửa backend/nới điều kiện |
| Bộ liên quan | 55 đạt, 0 fail/error/skip; BUILD SUCCESS; 2 phút 16 giây; `ux-cleanup-final.log` |
| Luồng liên tục bổ sung | 5 đạt, 0 fail/error/skip; BUILD SUCCESS; 24,785 giây; `ux-cleanup-flow.log` |
| Polish cuối | 11 test: 10 đạt, 0 assertion fail, 1 error ghi ảnh do Windows khóa file `student-768.png` (user-mapped section open); `ux-cleanup-polish.log` |
| Chạy lại riêng test ảnh | 1 đạt, 0 fail/error/skip; BUILD SUCCESS; 15,934 giây; `ux-cleanup-screenshot-retry.log` |

Có 56 test khác nhau: 55 trong bộ liên quan + 1 luồng liên tục mới. Lượt bổ sung có test trùng, không cộng thành số test khác nhau.

| Luồng | Bằng chứng browser tự động |
| --- | --- |
| Tổng quan → giao dịch | Test số dư chung/planner/history đạt; list dài cuộn, list ngắn không có nút cuộn vô dụng |
| Lịch sử | 5/trang, Next đổi dữ liệu; không bounded vertical scroll/nút cuộn; giữ search/filter/sort |
| Tạo plan học phí | AWAITING_APPROVAL, tổng 70.760.800 VND, đúng Bank A và người thụ hưởng; 0 payment trước duyệt |
| Xác nhận approval | Cancel: 0 payment; Accept: COMPLETED và receipt có 29.239.200 VND |
| Receipt/audit | Đúng plan, audit SANDBOX EXECUTED; test hiện có kiểm tra snapshot/idempotency |
| Delegated | Đổi mode bằng UI; tác vụ 250.000 VND thấp rủi ro COMPLETED và có receipt; không còn nút approval; tổng 2 payment sau học phí và tác vụ thấp rủi ro |
| Delegated + học phí | Workspace Delegated, review Approval; chưa duyệt không debit/receipt |
| Emergency Stop | Cảnh báo ở 4 tab; AGENT PAUSED và đúng câu backend; không approval; tác vụ mới BLOCKED, payment count không tăng; resume không tự thực thi |
| Ngôn ngữ/responsive | Anh/Việt ở rộng 360, 390, 768, 1024, 1366; nhãn mô phỏng trong panel và composer thấy được; không tràn ngang |
| Lý do chặn | English đúng backend; Việt dịch tương ứng; unknown giữ nguyên, không tạo hướng dẫn mới |
| Pending POST | Route fixture giữ response refresh quote: Processing, nút disabled, 1 request; trả response thì hết loading; không payment |
| Focus/dialog | Test focus trap mobile, bàn phím, dialog và tap target hiện có đạt |

Ảnh bằng chứng được xem trực tiếp để kiểm tra bố cục/chữ/badge. Các ID trong ảnh là dữ liệu synthetic test và thay đổi mỗi lần chạy.

### Walkthrough thủ công: BLOCKED

App local riêng đã khởi chạy thành công ở `http://localhost:8155`, PID 36320, H2 `ux_cleanup_manual`, LLM tắt. Không dùng database cá nhân hoặc Render.

Công cụ `mcp__cua_repl` thất bại hai lần:

1. `node_repl kernel exited unexpectedly`; `windows sandbox failed: helper_unknown_error: setup refresh had errors`.
2. Sau reset: `trusted Node process exited unexpectedly; kernel reset, rerun your request`.

Vì vậy chưa thao tác thủ công dashboard → plan → Approval/Delegated → result → audit → Emergency Stop. Không gán PASS cho manual. Luồng Chrome tự động ở trên và việc xem ảnh chỉ là bằng chứng riêng.

Checklist manual còn lại trên local:

1. Xem Tổng quan/tài khoản, lịch sử; thử phân trang, cuộn list và focus bàn phím.
2. Chọn học phí, tạo plan Bank A; review amount/recipient/account/channel/permission.
3. Approval: Cancel rồi Accept trong Sandbox; xem receipt và Audit Log.
4. Bật Delegated trong demo tools; thử tác vụ thấp rủi ro; học phí vẫn cần Approval.
5. Bật Emergency Stop; xem mọi tab/chat, thử tác vụ mới bị chặn rồi resume.
6. Anh/Việt, laptop/projector; điện thoại thật với bàn phím. Mobile hiện chỉ kiểm chứng browser mô phỏng.

## 4. UX cần backend và giới hạn ngoài phạm vi

- Không phát hiện yêu cầu sửa protected backend để hoàn thành các điểm audit đã duyệt; diff các file production Java/config/schema/dependency rỗng.
- Chi tiết audit là nội dung kỹ thuật backend cung cấp; chỉ định dạng và dịch nhãn. Nếu muốn lời giải thích nghiệp vụ riêng cho từng event, cần chốt nội dung/contract trong task khác; không tự viết lời giải thích tài chính phía frontend.
- Provider latency/timeout và nghiệm thu Render không nằm trong task này.
- Manual browser và điện thoại thật chưa xác minh. Không tuyên bố toàn bộ nghiệm thu hoàn tất hoặc production ready.
