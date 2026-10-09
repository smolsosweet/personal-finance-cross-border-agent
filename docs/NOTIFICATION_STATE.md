# Trạng thái thông báo và đã xem

Ngày: 09/10/2026. Commit nền: `15c4c01`.

## Lỗi và thay đổi

Nguồn `CrossBorderService.tuitionInsight()` luôn dựng lời nhắc cần phê duyệt cho hóa đơn đang chọn, kể cả khi đã có giao dịch Sandbox hoàn tất. Việc thanh toán thực tế không bị sai; thông báo không xét trạng thái thực thi.

- Khi hóa đơn đang chọn đã có giao dịch hoàn tất, thông báo được tạo từ quan hệ bill → action → receipt hiện có: “Thanh toán du học đã hoàn tất”, không yêu cầu duyệt thêm, liên kết đến đúng plan/biên nhận.
- Hóa đơn chưa thanh toán vẫn giữ lời nhắc và quy trình phê duyệt hiện có.
- Mỗi thông báo có “Đánh dấu đã xem”; có thêm “Đánh dấu tất cả đã xem”. Đã xem không hoàn tất tác vụ, không phê duyệt, không thay đổi số dư; mục và liên kết xử lý vẫn hiện.
- Badge đếm nội dung chưa xem. Mở popup không tự đánh dấu đã xem.
- Read state lưu trong `localStorage` của trình duyệt, tách khỏi dữ liệu tài chính dùng chung. Phiên trình duyệt mới vẫn có trạng thái đọc riêng. Không đồng bộ qua tài khoản/thiết bị khi chưa có đăng nhập.
- Bản nội dung thay đổi trở thành chưa xem; receipt mới là một cập nhật khác lời nhắc trước đó. Chỉ fingerprint UI được lưu, không lưu message tài chính hay secret.
- Storage bị chặn: giữ trạng thái trong tab hiện tại và thông báo rõ giới hạn lưu trữ.
- Cập nhật nội dung qua thao tác AJAX/tải lại; chưa có push/poll giữa các thành viên team.
- Không sửa execution, Policy Guard, AI, schema, configuration hoặc dependencies. Thay đổi backend chỉ ở việc đọc trạng thái để tạo thông báo.

## Test thực chạy

Chrome headless qua Playwright, Spring Boot local cổng 8164, H2 riêng `notification_ui`, LLM mock/disabled. Không gọi Gemini, không dùng Render. Không phải kiểm tra thủ công hoặc điện thoại thật.

```powershell
mvn '-Dtest=NotificationCenterPlaywrightTest' test
```

Lượt đầu: 8 ERROR do template truy cập field tùy chọn không tồn tại trên map; chưa chạy được scenario. Đã sửa đọc field bằng `Map.get`. Lượt tiếp: 7 PASS, 1 ERROR do selector navigation bắt cả bản trùng ẩn sau khi attention card được đổi từ anchor sang div để chứa nút đã xem.

Sau sửa selector chỉ trỏ mục hiển thị:

```powershell
mvn '-Dtest=NotificationCenterPlaywrightTest#opensFromEveryTabAndRoutesToTheCorrectReviewWithoutFinancialChanges' test
```

1/1 PASS, BUILD SUCCESS.

Chụp lại thông báo hoàn tất trong viewport, chạy riêng đúng ca:

```powershell
mvn '-Dtest=NotificationCenterPlaywrightTest#actualVcbApprovalReplacesTheStaleReminderWithTheExactReceiptAndANewUnseenUpdate' test
```

1/1 PASS, BUILD SUCCESS.

Tổng **8 ca khác nhau có kết quả PASS qua các lượt**; không phải một lượt full suite 8/8. Không chạy lại test không liên quan.

| Ca | Kết quả |
|---|---|
| Bell gọn, dedup, responsive 360/390/768/1366/1920px, VI/EN | PASS |
| Mọi tab mở đúng khu xử lý, không đổi tài chính | PASS |
| Event cập nhật thông báo qua AJAX, không POST thừa | PASS |
| Escape/backdrop/focus trap, inline Emergency Stop/payment blocks còn hiện | PASS |
| Đã xem giữ qua reload, không approve draft, browser mới tách read state | PASS |
| Mark all giữ mục, bàn phím đúng; nội dung event mới hiện chưa xem lại | PASS |
| Approval VCB qua dialog UI → receipt đúng → thay lời nhắc cũ bằng hoàn tất → đọc không thêm thanh toán | PASS |
| Storage bị chặn vẫn dùng được, nêu rõ chỉ giữ trong tab | PASS |

Snapshot tài chính trước/sau đánh dấu đọc bằng nhau. Ca VCB tạo đúng một giao dịch Sandbox qua approval riêng, notification mở đúng receipt, không còn nút approval cho plan đã hoàn tất. Tiền và policy không được thay đổi bởi nút đã xem.

Maven thực tế: `C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd`.

## Bằng chứng

Ảnh và logs ở `docs/notification-state/`; log lỗi template ban đầu lưu trích đoạn có nhãn, các lượt sau lưu đầy đủ. Ảnh lịch sử trong `docs/notifications/` được giữ nguyên.

## Sau deploy

Chưa nghiệm thu Render hoặc điện thoại thật cho bản này. Xác nhận SHA Live, tải lại, mở chuông trên hóa đơn đã thanh toán: phải hiện hoàn tất và đúng biên nhận. Thử đánh dấu một mục đã xem rồi tải lại, xác nhận badge giảm và trạng thái vẫn giữ. Không cần reset hoặc thanh toán lại trên workspace chung.
