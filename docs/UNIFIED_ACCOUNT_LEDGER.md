# FinBridge — Sổ tài khoản chung và khối danh sách cố định

Ngày kiểm chứng: 05/10/2026. Phạm vi: account ledger, giao dịch, kế hoạch/ngân sách, chat, Payment Sandbox và giao diện liên quan. Dữ liệu synthetic; không gọi Gemini/Ollama thật, không dùng API ngân hàng hay tiền thật; không push/deploy.

## Kết quả cho người dùng

- Tổng quan, Tài khoản & tiền mặt, planner, chat và thanh toán dùng cùng nguồn số dư. Không cộng một tài khoản lần nữa chỉ vì nó có mặt ở nhiều màn hình.
- Seed mới có **6 nguồn tiền cá nhân**: Bank A Everyday (100 triệu), Vietcombank (82 triệu), Techcombank (45 triệu), MoMo (12 triệu), Alipay (75 triệu), Emergency fund tại Bank A (1 triệu). Tổng VND: **315 triệu**. Đây là số tiền mô phỏng, không phải ngân hàng thật.
- Nguồn tiền nhập tay/tiền mặt xuất hiện trong Tổng quan và câu trả lời số dư, được ghi rõ là nhập thủ công. Chúng không có quyền thực thi thanh toán trực tuyến.
- Mỗi tiền tệ được hiển thị/tổng hợp riêng. Số dư ngân sách là hạn mức chi tiêu, không phải số dư tiền.
- Bank A có hai tài khoản. Hỏi chung về Bank A phải làm rõ tài khoản; hỏi rõ Bank A Everyday dùng đúng tài khoản thanh toán.
- Thanh toán cập nhật sổ chung đúng một lần sau approval. Ví dụ seed Bank A: 100.000.000 − 70.760.800 = **29.239.200 VND**; tổng tiền cá nhân còn **244.239.200 VND**.
- Biên nhận giữ số dư tại thời điểm giao dịch. Sự kiện ngân hàng sau đó thay đổi số dư hiện tại, không viết lại biên nhận cũ.
- Chuyển nội bộ vào quỹ dự phòng ghi có tài khoản tiết kiệm thật trong sổ demo; tổng tiền cá nhân không đổi.
- Lịch sử giao dịch hiển thị tên và tham chiếu tài khoản nguồn.
- Bỏ composer/chat history trùng trong Tổng quan. Các nút Hỏi FinBridge mở cùng trợ lý toàn cục. Công cụ hội thoại chẩn đoán trong Environment tools vẫn chỉ là công cụ demo.

## Nguồn dữ liệu và chuyển đổi database

`financial_accounts` là nơi duy nhất lưu số dư hiện tại. `account_scope=PERSONAL` và `owner_profile_id=1` giới hạn danh mục thuộc hồ sơ synthetic hiện tại; SYSTEM là người nhận, không thuộc tổng tiền của người dùng.

`payment_source_accounts` giữ khả năng sử dụng, verification, lựa chọn và ánh xạ ID; không lưu số dư. ID payment cũ `PAYER_VND` ánh xạ về `CHECKING`, `EMERGENCY_VND` về `SAVINGS` để giữ tương thích với plan/hash/receipt cũ. `sandbox_accounts` trở thành **view chỉ đọc**, không phải một sổ số dư thứ hai.

`AccountLedgerService.migrateLegacyBalances()` chạy khi khởi động. Với seed legacy tiêu chuẩn, CHECKING và PAYER từng chứa cùng 100 triệu ban đầu. Chuyển đổi lấy số dư payment hiện tại cộng phần thay đổi cá nhân (`CHECKING − 100 triệu`), không cộng hai số dư seed. Credit quỹ dự phòng cũ được cộng vào SAVINGS. Không sửa action hash, approvals, receipts hoặc ledger lịch sử. Connection status cũ được giữ; currency/ID xung đột bị từ chối thay vì đoán hoặc tự reset.

Bước chuyển đổi này dành cho schema/demo legacy đã biết của repository; không phải cơ chế migration dữ liệu ngân hàng tùy ý. Database local của người dùng chỉ được chuyển đổi khi họ khởi động lại ứng dụng; kiểm chứng trong task dùng database test riêng.

Khóa hàng tài khoản được dùng chung giữa bank event và Sandbox execution; execution kiểm tra lại Policy Guard sau khóa. Approval, idempotency, recipient/quote/corridor/currency/limit/safety-buffer checks được giữ. Startup thiếu nguồn tài khoản của một workspace đã tồn tại sẽ báo lỗi, không tự xóa lịch sử payment. Reset chủ động vẫn khôi phục seed.

Projection giữ trước khoản planner gắn với tài khoản nguồn đang xét; không trừ khoản thuộc tài khoản khác. Runway vẫn dùng mức chi tháng được người dùng xác nhận và không trừ planner lần nữa, tránh trùng tiền thuê nhà/ăn uống đã có trong mức chi tháng.

## Giao diện

Các vùng danh sách tài khoản, kế hoạch, ngân sách, mục cần xem xét, Proactive Feed, lịch sử giao dịch/thanh toán và Audit Log có chiều cao theo breakpoint, cuộn bên trong và nút **Cuộn xuống**. Có focus bàn phím, scrollbar, hỗ trợ reduced motion, và giữ vị trí cuộn sau cập nhật/chat. Bộ lọc và phân trang cũ được giữ.

- Tài khoản/kế hoạch: 480px desktop, 420px mobile.
- Lịch sử giao dịch: 480px desktop, 420px mobile; vẫn 5 giao dịch mỗi trang.
- Các danh sách chi tiết/audit: 320px desktop, 300px mobile.
- Snapshot tài khoản: 240px; snapshot ngân sách: 200px.
- Hóa đơn và so sánh kênh tiếp tục dùng carousel ngang; dialog vẫn cuộn theo viewport.

Ảnh stress test với 18 nguồn tiền nhập tay thêm vào seed: [Desktop](unified-accounts/accounts-desktop.png), [Mobile giả lập](unified-accounts/accounts-mobile.png).

## Kiểm chứng thực tế

**258 test liên quan đã có kết quả PASS mới nhất**, gồm 187 backend/MockMvc và 71 test Chrome/Playwright. Đây là tổng test khác nhau trong 29 class, không cộng các lần chạy lại. [Manifest kết quả](unified-accounts/test-results.json) chứa chỉ class/count/timestamp, không sao chép secret hoặc toàn bộ system properties.

Đợt hồi quy rộng đầu tiên chạy 255 test, có 3 assertion lỗi: kỳ vọng 5 nguồn thay vì 6, câu chữ thông báo tài khoản và tiêu đề lịch sử UI cũ. Các assertion được cập nhật theo contract mới, rồi chạy lại đúng phần liên quan. Hai test mới sau đó kiểm tra startup không ghi đè bank event và không xóa receipt khi dữ liệu thiếu, nâng tổng thành 257. Một test bổ sung xác nhận nguồn mất kết nối có lý do rõ và không thể thực thi plan cũ, nâng tổng cuối lên 258. Không bỏ/skip test hay nới guard để đạt kết quả.

Lệnh thực tế (chạy ở root repository, Maven 3.9.16/JDK 21):

```powershell
mvn -Dtest=UnifiedAccountLedgerIntegrationTest,TransactionServiceTest,FinanceWorkspaceIntegrationTest,PaymentSourceIntegrationTest,PaymentWorkflowIntegrationTest,PhaseTwoIntegrationTest,PhaseThreeIntegrationTest,PhaseFourIntegrationTest,PhaseFiveIntegrationTest,PersonalFinanceAiIntegrationTest,RunwayIntegrationTest,GlobalAssistantIntegrationTest,SessionConversationIntegrationTest,StudentExpenseIntegrationTest,ProductionUiIntegrationTest,UnifiedAccountsPlaywrightTest,AssistantPanelPlaywrightTest,PersonalFinanceAiPlaywrightTest,FinanceOverviewPlaywrightE2ETest,UiProductRefinementPlaywrightTest,UiReviewFollowupPlaywrightTest,AssistantAnswerPresentationPlaywrightTest,GlobalAssistantPlaywrightTest,PaymentSourcePlaywrightE2ETest,PaymentWorkflowPlaywrightE2ETest,ContextualConversationPlaywrightTest,StudentExpensePlaywrightE2ETest,RunwayPlaywrightTest,FinBridgePlaywrightE2ETest test
# Đợt rộng: 255 test, 3 assertion thất bại như mô tả phía trên.

mvn -Dtest=GlobalAssistantIntegrationTest,PaymentSourceIntegrationTest,ProductionUiIntegrationTest,UnifiedAccountLedgerIntegrationTest,UnifiedAccountsPlaywrightTest,AssistantAnswerPresentationPlaywrightTest test
# 51/51 PASS — xác nhận assertion cập nhật và nguồn chung.

mvn -Dtest=UnifiedAccountLedgerIntegrationTest,PhaseFourIntegrationTest,PhaseFiveIntegrationTest,PaymentWorkflowIntegrationTest,PaymentSourceIntegrationTest,UnifiedAccountsPlaywrightTest test
# 62/62 PASS — startup/migration, trạng thái kết nối, policy và balance lock.

mvn -Dtest=UnifiedAccountsPlaywrightTest,FinBridgePlaywrightE2ETest,PaymentWorkflowPlaywrightE2ETest,UiReviewFollowupPlaywrightTest test
# 22/22 PASS — vùng cuộn lịch sử/audit, filter, payment, mobile và Việt–Anh.

mvn -Dtest=GlobalAssistantIntegrationTest,GlobalAssistantPlaywrightTest,UnifiedAccountLedgerIntegrationTest,UnifiedAccountsPlaywrightTest test
# 46/46 PASS — nhiều tài khoản Bank A, làm rõ đúng đối tượng và bản UI cuối.

mvn -Dtest=UnifiedAccountLedgerIntegrationTest,PaymentSourceIntegrationTest,UnifiedAccountsPlaywrightTest test
# 18/18 PASS — nguồn mất kết nối có lý do rõ và không thể thực thi plan cũ.
```

Trong môi trường này `mvn` được gọi bằng đường dẫn `C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd`, output được redirect vào `target/unified-*.log`. Các con số trên là kết quả đã quan sát, không phải kỳ vọng.

Các ca mới có bằng chứng: bank event/duplicate, tuition trước-sau approval/idempotency, bank event đồng thời payment, biên nhận bất biến, chuyển nội bộ bảo toàn tổng tiền, tiền mặt tạo/sửa/archive, reservations đúng tài khoản, reset ba lần, migration/replay schema, giữ connection status, startup thiếu dữ liệu không xóa history, danh mục nguồn chung ở UI/chat, fixed height 24 tài khoản, nút cuộn không nhảy trang, giữ scroll sau chat, mobile ngang không tràn trang.

## File và giới hạn

Production: `AccountLedgerService`, `TransactionService`, `FinanceWorkspaceService`, `PhaseFourService`, `DemoDataService`, `GlobalAssistantQueries`, `PersonalFinanceInsights`, `LivingExpenseRunwayService`, `SessionConversationService`, `ConversationPresentation`, `schema.sql`, `home.html`, `app.js`, `app.css`. Test: thêm `UnifiedAccountLedgerIntegrationTest`, `UnifiedAccountsPlaywrightTest` và điều chỉnh fixture/assertion/selectors liên quan nguồn chung; adapter/provider/strict parser không đổi.

Không chạy lại provider/startup/model tests không liên quan. Gemini/Ollama live requests: **0**. Các browser chat tests dùng mock model; các live IT tùy chọn chỉ được cập nhật fixture/selector, chưa được thực thi trong task này. Chrome mobile là giả lập, chưa kiểm tra điện thoại thật. Chưa nghiệm thu bản Render sau thay đổi; chưa push hay deploy. Demo vẫn synthetic, dùng chung hồ sơ, không có đăng nhập/tách dữ liệu người dùng; thay đổi này không biến demo thành ứng dụng production cho tiền thật.

## Chạy bản local mới

Dừng app cũ bằng Ctrl+C trong terminal đang chạy, rồi ở root project và terminal có GEMINI_API_KEY:

```cmd
scripts\start-gemini.cmd -Port 8124
```

Mở http://localhost:8124, tải lại trình duyệt, kiểm tra cùng danh mục nguồn ở Tổng quan/Tài khoản/chat, rồi xem số dư sau một payment Sandbox được phê duyệt. Không reset dữ liệu dùng chung trên Render trong đợt này.
