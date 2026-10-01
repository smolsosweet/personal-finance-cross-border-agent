# Luồng Tác vụ & Thanh toán — 01/10/2026

## Phạm vi

Hoàn thiện luồng giao diện production trên dữ liệu mô phỏng: chọn hóa đơn giáo dục → so sánh kênh → tạo kế hoạch → kiểm tra → phê duyệt → Payment Sandbox → biên nhận. Không có kết nối ngân hàng, tiền thật, LLM hay tính năng chuyển tiền cá nhân mới.

## Cách sử dụng

1. Vào **Tài chính du học**, chọn hóa đơn đang hoạt động và đã xác minh.
2. Chọn **Tạo kế hoạch** tại kênh có đủ số dư và giữ được vùng an toàn. Màn hình thanh toán mở sau bước này; chưa trừ tiền.
3. Kiểm tra tên người thụ hưởng, ngân hàng/mã định tuyến, tài khoản nhận, mã tham chiếu, tài khoản nguồn và kênh. Hai cột thông tin hiển thị phí, tỷ giá, tổng tiền trừ và số dư dự kiến còn lại.
4. Kiểm tra đếm ngược báo giá, thời gian quyết toán và cảnh báo hạn thanh toán. Báo giá hết hạn khóa nút phê duyệt ngay, không cần F5.
5. Bấm **Phê duyệt và thanh toán trong Sandbox**. Xác nhận đúng thông tin đã khóa; backend kiểm tra lại Policy Guard rồi mới thực thi.
6. Biên nhận ghi đúng kế hoạch, mã giao dịch, tổng tiền trừ, phần quy đổi, phí và tiền ghi có. Phí/quy đổi là thành phần của tổng tiền trừ, không được trừ thêm lần nữa.
7. Mở lại qua **Xem biên nhận** trên hóa đơn hoặc **Lịch sử kế hoạch thanh toán**. Không cần tạo thanh toán mới để xem lịch sử.

**Hủy kế hoạch** chỉ dành cho kế hoạch chưa thực thi và không hủy hóa đơn. Kế hoạch đã hoàn tất không thể hủy/sửa. Hóa đơn đã trả không còn hiển thị kênh sẵn sàng hoặc nút tạo thanh toán mới.

**Dừng khẩn cấp** ở header cố định, dùng được khi công cụ demo đang đóng. Nó chặn thực thi mới và không hoàn tác khoản đã hoàn tất. **Tiếp tục tác vụ** không tự trả tiền: khoản giáo dục vẫn cần người dùng phê duyệt.

**Công cụ demo và thiết lập chính sách** chứa hội thoại theo quy tắc, thử prompt injection, hạn mức/ủy quyền cho tác vụ rủi ro thấp, offline fallback, thử idempotency và nhật ký demo toàn hệ thống. Nhật ký bên ngoài chỉ thuộc kế hoạch đang xem. Dashboard có mục demo thu gọn để tạo ví dụ rủi ro thấp trước khi có kế hoạch giáo dục.

## Quy tắc dữ liệu và thực thi

- POST tạo kế hoạch ràng buộc `expenseId`, `billVersion`, `quoteId`, `sourceAccountId`, `channel`. Yêu cầu từ lựa chọn/báo giá cũ bị từ chối.
- Bảng `action_payment_snapshots` lưu bản chụp hóa đơn, người thụ hưởng/ngân hàng, nguồn tiền, báo giá và thời gian xử lý. Số tiền/phí lưu trong `action_plans`; số dư thực thi lưu trong receipt.
- Yêu cầu giống nhau mở lại kế hoạch đang chờ. Thay kênh/báo giá tạo kế hoạch thay thế và thu hồi phê duyệt cũ.
- Sửa/lưu trữ/hủy hóa đơn vô hiệu kế hoạch đang chờ. Policy Guard kiểm tra hóa đơn thuộc kế hoạch, độc lập với hóa đơn đang chọn trên màn hình.
- Khóa hàng `agent_policy FOR UPDATE` tuần tự hóa thay đổi tài chính trong demo một người dùng, cùng với unique idempotency/action keys. Test concurrency xác nhận tạo và phê duyệt đồng thời không nhân đôi.
- Một hóa đơn đã có receipt không được thanh toán bằng kế hoạch khác. Lặp lại cùng kế hoạch hoàn tất trả biên nhận cũ, không trừ tiền thêm.
- Query `?action=ID` mở kế hoạch cụ thể. AJAX mang `X-Workspace-Action` giữ kế hoạch lịch sử khi thao tác ngoài thanh toán; tạo/mở kế hoạch mới thay ngữ cảnh, reset xóa ngữ cảnh. Header chỉ chọn màn hình, không cấp quyền thực thi.
- Nút biên nhận của hóa đơn lấy từ toàn bộ kế hoạch hoàn tất, độc lập danh sách 30 kế hoạch gần nhất.
- Kế hoạch giáo dục cũ thiếu snapshot bị chặn `PLAN SNAPSHOT MISSING`; cần tạo và phê duyệt lại. Receipt cũ vẫn đọc được; không dựng timestamp/báo giá chưa được lưu.
- Học phí và các khoản giáo dục luôn cần Approval. AI và chế độ Delegated không bỏ qua được quy tắc này.

## Các lệnh đã chạy và kết quả thực tế

Môi trường: Windows, Java 21.0.6, Maven 3.9.16, Spring Boot 3.5.6, H2 riêng cho từng test, Playwright Java và Chrome đã có. Không cài dependency.

### Test tích hợp và hồi quy liên quan

```powershell
mvn "-Dtest=PaymentWorkflowIntegrationTest,PhaseFourIntegrationTest,PhaseFiveIntegrationTest,PaymentSourceIntegrationTest,StudentExpenseIntegrationTest,ProductionUiIntegrationTest" test
```

Kết quả: **53 chạy, 0 failure, 0 error, 0 skipped — BUILD SUCCESS**.

Sau khi bổ sung giữ ngữ cảnh AJAX và mở receipt ngoài giới hạn 30 kế hoạch:

```powershell
mvn "-Dtest=PaymentWorkflowIntegrationTest" test
```

Kết quả cuối: **17 chạy, 0 failure, 0 error, 0 skipped — BUILD SUCCESS**. Có 15 test trùng nhóm trước, nên tổng là **55 test tích hợp khác nhau**.

### Browser E2E liên quan

```powershell
mvn "-Dtest=PaymentWorkflowPlaywrightE2ETest,StudentExpensePlaywrightE2ETest#inPlaceFormsKeepApprovalAndSandboxExecutionControlledAndEmergencyStopBlocksNewActions+expenseAndCompactChannelFlowWorksInBrowser,PaymentSourcePlaywrightE2ETest#accountsCanBeFilteredAndSelectedWhileSuggestionsStayReferenceOnly,FinBridgePlaywrightE2ETest#fixedTuitionCorridorAndEligibleChannelsAreVisible+bankBShowsLowerReferenceRateButCannotBeSelected+recipientMismatchAndExpiredQuoteAreBlockedInBrowserFlow+approvedPaymentCreatesMultiCurrencyReceiptAndAuditTrail+injectionEmergencyStopAndResetReplayRemainSafe" test
```

Kết quả lần này: **13 chạy, 11 đạt, 2 error selector**. Cả **5 E2E mới đều đạt**. Hai error do `summary` chọn cả tiêu đề công cụ demo và tiêu đề nhật ký lồng bên trong; sửa selector thành `:scope > summary`.

Chạy lại đúng hai trường hợp đó và một luồng receipt sau khi chỉnh nội dung trạng thái hoàn tất:

```powershell
mvn "-Dtest=StudentExpensePlaywrightE2ETest#inPlaceFormsKeepApprovalAndSandboxExecutionControlledAndEmergencyStopBlocksNewActions,FinBridgePlaywrightE2ETest#injectionEmergencyStopAndResetReplayRemainSafe,PaymentWorkflowPlaywrightE2ETest#approvingExactPlanShowsItsReceiptAndCompletedSnapshotSurvivesNewQuotes" test
```

Kết quả cuối: **3 chạy, 0 failure, 0 error, 0 skipped — BUILD SUCCESS**. Tổng **13 luồng E2E khác nhau đều đạt sau sửa**, không cộng lại trường hợp chạy lặp.

Các lượt đầu cũng phát hiện lỗi JavaScript do input `name=action` che thuộc tính `form.action`; đã sửa lấy URL bằng `getAttribute('action')`. Test chờ hoàn tất cập nhật trước khi đọc mã kế hoạch; test mới kiểm tra lỗi JavaScript.

```powershell
node --check src/main/resources/static/app.js
git diff --check
```

Đạt. Cảnh báo Git về LF/CRLF không phải lỗi kiểm tra.

## Khởi động và giới hạn xác minh

Cổng 8080 đang được sử dụng, nên bản kiểm tra riêng chạy tại `http://localhost:8086` bằng dữ liệu seed trong bộ nhớ:

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8086 --spring.datasource.url=jdbc:h2:mem:payment_workflow_manual;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
```

Ứng dụng đã khởi động thành công. Dữ liệu này không ghi đè file H2 đang dùng ở cổng 8080. Khi chạy dự án thường ngày, dùng `mvn spring-boot:run` và restart tiến trình cũ để nạp code mới.

Đã xem ảnh browser desktop review/receipt và mobile tiếng Việt do Playwright tạo ở `target/payment-review-desktop.png`, `target/payment-receipt-desktop.png`, `target/payment-review-mobile-vi.png`. Các ảnh và báo cáo Surefire là output build, không đưa vào Git.

**Kiểm tra tương tác thủ công qua công cụ trình duyệt: BLOCKED**, thử hai lần đều lỗi `node_repl kernel exited unexpectedly — windows sandbox failed: helper_unknown_error: setup refresh had errors`. Không tuyên bố bước thủ công này đã đạt. Cách bổ sung tối thiểu: khôi phục kết nối công cụ browser hoặc người dùng mở localhost và thực hiện các bước ở trên. Playwright E2E đã thực sự chạy trên Chrome với ứng dụng local.

Không phát hiện lỗi chức năng còn mở trong phạm vi các test đã chạy. Không chạy lại các test giao dịch/phân loại/ngân sách không liên quan và không bắt đầu phase AI.
## Tệp thay đổi

Backend và dữ liệu:
- `src/main/java/com/example/finance/PhaseFourService.java`
- `src/main/java/com/example/finance/PhaseOneController.java`
- `src/main/java/com/example/finance/CrossBorderService.java`
- `src/main/resources/schema.sql`

Giao diện Việt–Anh:
- `src/main/resources/templates/home.html`
- `src/main/resources/static/app.js`
- `src/main/resources/static/app.css`

Test mới và hồi quy liên quan:
- `src/test/java/com/example/finance/PaymentWorkflowIntegrationTest.java`
- `src/test/java/com/example/finance/PaymentWorkflowPlaywrightE2ETest.java`
- `src/test/java/com/example/finance/PaymentSourceIntegrationTest.java`
- `src/test/java/com/example/finance/PaymentSourcePlaywrightE2ETest.java`
- `src/test/java/com/example/finance/StudentExpenseIntegrationTest.java`
- `src/test/java/com/example/finance/StudentExpensePlaywrightE2ETest.java`
- `src/test/java/com/example/finance/ProductionUiIntegrationTest.java`
- `src/test/java/com/example/finance/FinBridgePlaywrightE2ETest.java`

Hướng dẫn:
- `README.md`
- `docs/PAYMENT_WORKFLOW.md`