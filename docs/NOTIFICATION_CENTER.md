# Chuông thông báo FinBridge

> Báo cáo lịch sử cho phiên bản chuông ban đầu. Trạng thái đã xem và thông báo sau thanh toán đã được bổ sung; xem [NOTIFICATION_STATE.md](NOTIFICATION_STATE.md).

Ngày kiểm chứng: 09/10/2026. Commit nền: `29c88bb`.

## Thay đổi

- Gom Action Center và Proactive Feed khỏi Tổng quan vào chuông trên header, sử dụng từ mọi tab.
- Số trên chuông là số nội dung hiện cần xem xét, **không phải số tin chưa đọc**. Mở/đóng không tự đánh dấu công việc hoàn tất.
- Gộp mục xem xét giao dịch bị lặp giữa hai nguồn; ưu tiên cao hiển thị trước. Nguồn dữ liệu, giả định và liên kết xử lý được giữ lại.
- Bỏ các shortcut điều hướng chung khỏi danh sách thông báo; các tab vẫn có sẵn trong navigation.
- Danh sách cuộn trong popup có chiều cao giới hạn. Đóng bằng nút ×, Escape hoặc bấm vùng ngoài; focus trở lại chuông. Tab/Shift+Tab giữ focus trong popup.
- Số lượng/nội dung được cập nhật theo dữ liệu backend sau thao tác AJAX và khi tải trang. **Chưa có push notification hoặc cập nhật nền giữa các người dùng**; chưa thêm lưu trạng thái đã đọc.
- Giữ cảnh báo chặn thanh toán, báo giá hết hạn và trạng thái dừng ở ngay màn hình thanh toán. Xem thông báo không tạo plan, không cấp quyền, không thực thi.
- Không thay đổi Java nghiệp vụ, AI, policy, schema, cấu hình, dependencies hoặc nguồn dữ liệu tài chính.

## Browser automation đã chạy

Chrome headless thực, Spring Boot local, H2 riêng. Test mới dùng cổng 8164/database `notification_ui`. VI/EN; 360, 390, 768, 1366, 1920px. Không gọi Gemini (model mock/disabled), không tác động Render hoặc điện thoại thật.

```powershell
mvn '-Dtest=NotificationCenterPlaywrightTest,WorkspaceListScrollPlaywrightTest#overviewStaysCompactWithAttentionInTheGlobalBell+transactionHistoryUsesOnlyPaginationOnMobile,BusinessUxPlaywrightTest#finalReviewHasLockedFieldsAndCancelEscapeDoNotApprove,UiCleanupDefectRegressionPlaywrightTest#notificationsUseLayoutSpaceAndNeverCoverPolicyApprovalOrChat' test
```

Lượt đầu: **8 test, 6 PASS, 2 FAIL**. Một selector test chưa loại bản trùng bị ẩn; một lỗi thực tế là listener chuông chỉ được đăng ký khi mở Tổng quan nên tải trực tiếp plan bị chặn không mở được chuông.

```powershell
mvn '-Dtest=NotificationCenterPlaywrightTest' test
```

Lượt tiếp: **4 test, 3 PASS, 1 FAIL** (lỗi đăng ký listener còn lại). Sau khi chuyển listener sang khởi tạo trang duy nhất và bổ sung vòng focus bàn phím, lượt cuối **4/4 PASS, BUILD SUCCESS**. Các assertion không bị bỏ/nới lỏng để đạt.

**Tổng 8 ca khác nhau đã đạt qua các lượt**, không phải một lần chạy 8/8 trên bản cuối:

| Ca | Kết quả |
|---|---|
| Chuông thay các khối lớn, count không trùng, responsive VI/EN | PASS |
| Từ mọi tab mở đúng phần xử lý, snapshot tài chính giữ nguyên | PASS |
| Sự kiện ngân hàng cập nhật nội dung không F5, chỉ một POST mô phỏng | PASS |
| Plan bị chặn vẫn mở chuông; Escape/bấm ngoài/focus; cảnh báo inline còn hiện | PASS |
| Tổng quan gọn trên desktop/mobile mô phỏng | PASS |
| Lịch sử chỉ phân trang, tối đa 5 giao dịch/trang | PASS |
| Popup phê duyệt giữ chi tiết đúng, nút không bị che, hủy/Escape không trả tiền | PASS |
| Notice/header/chat không che quyền hạn hoặc nút quan trọng | PASS |

Lệnh thực tế gọi Maven tại `C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd`. Log nguyên bản của từng lượt và ảnh cuối được lưu ở `docs/notifications/`. Ảnh lịch sử bị test tái tạo được khôi phục; không ghi đè bằng chứng cũ.

## File

`src/main/resources/templates/home.html`, `src/main/resources/static/app.js`, `src/main/resources/static/app.css`, `src/test/java/com/example/finance/NotificationCenterPlaywrightTest.java`, `src/test/java/com/example/finance/WorkspaceListScrollPlaywrightTest.java`, báo cáo/ảnh/log này.

## Cần kiểm tra sau deploy

Xác nhận SHA Live rồi tải lại bỏ cache. Bấm chuông từ Tổng quan/Giao dịch/Thanh toán; bấm liên kết để đến đúng phần; đóng rồi mở lại. Trên điện thoại thật kiểm tra thao tác cuộn/đóng. Đợt này chưa kiểm chứng Render, screen reader hoặc thiết bị thật; không chạy full suite không liên quan.
