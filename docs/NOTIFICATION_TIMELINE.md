# Thông báo theo thời gian và trạng thái đã xem

Ngày kiểm chứng: 09/10/2026. Commit nền: `3994558`.

## Cách hoạt động

- Mặc định **Tất cả**, mới nhất trước. Ưu tiên cao/thấp và trạng thái đã xem không làm thay đổi thứ tự; thời gian bằng nhau được sắp ổn định theo định danh.
- **Cần xử lý**: giao dịch cần xác nhận, kế hoạch quá hạn, hóa đơn cần chuẩn bị/xác minh. Đã xem vẫn còn tác vụ cần xử lý, không hoàn tất hay phê duyệt.
- **Cập nhật & gợi ý**: thanh toán hoàn tất, tiến độ ngân sách, tiền đã giữ trước, đệm an toàn. Không cần người dùng thực hiện tác vụ tài chính chỉ để đọc thông tin.
- Với cập nhật/gợi ý, bấm nội dung, liên kết hoặc nhấn Enter/Space khi focus thẻ sẽ đánh dấu đã xem. Mở nguồn dữ liệu bằng `details` không tự đánh dấu. Với tác vụ cần xử lý, dùng nút **Đánh dấu đã xem** riêng.
- Khi đã xem, chỉ hiện **Đã xem**, ẩn nút đánh dấu trùng lặp. Có **Đánh dấu tất cả đã xem**, áp dụng cho toàn bộ danh sách, kể cả mục đang bị bộ lọc ẩn.
- Badge đếm mọi nội dung chưa xem, không chỉ bộ lọc đang mở. Thay đổi nội dung hoặc thời điểm nguồn trở thành bản cập nhật chưa xem mới.
- Thông báo hoàn tất của hóa đơn đang chọn dùng đúng receipt thực tế. Test thanh toán VCB chứng minh cập nhật này đứng đầu, mở đúng biên nhận, không còn nhắc duyệt thêm.

## Thời gian và giới hạn

Thời gian trên thẻ là thời điểm cập nhật **dữ liệu nguồn**, không phải giờ tải lại trang:

| Nguồn | Thời gian dùng |
|---|---|
| Thanh toán hoàn tất | `sandbox_transactions.created_at` |
| Hóa đơn cần chuẩn bị/xác minh | `international_bills.updated_at` |
| Giao dịch cần xem xét | Lần review hoặc nhận sự kiện gần nhất trong nhóm chờ |
| Tiến độ ngân sách | Lần review/nhận giao dịch chi đã phân loại gần nhất của danh mục/kỳ đang báo cáo |
| Khoản giữ trước/kế hoạch | Thời điểm cập nhật gần nhất của planner |
| Đệm an toàn | Thời điểm cập nhật số dư tài khoản nguồn của thông tin này |

Nếu thiếu timestamp, ghi rõ chưa có thời gian và xếp cuối; không gán thời gian giả. Bảng ngân sách hiện không lưu thời điểm sửa hạn mức, nên thời gian thẻ ngân sách phản ánh giao dịch nguồn, không phải thời điểm sửa limit. Đây là danh sách thông tin hiện tại, chưa phải kho lịch sử notification độc lập; thông báo học phí vẫn theo hóa đơn đang chọn.

Read state chỉ lưu fingerprint UI trong trình duyệt, chưa đồng bộ tài khoản/thiết bị. Cập nhật hiển thị qua thao tác workspace hoặc tải lại, chưa có push/poll giữa teammates. Phiên bản fingerprint mới có thể làm các nội dung từng xem ở bản cũ hiện chưa xem một lần.

Không thay đổi schema, dependency, AI, Policy Guard, approval hay execution. Không thao tác Render.

## Test thực chạy

```powershell
& 'C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd' '-Dtest=NotificationCenterPlaywrightTest' test
```

**11/11 PASS**, Failures 0, Errors 0, Skipped 0, BUILD SUCCESS. Test 36,22 giây; Maven tổng 47,667 giây. Chạy đúng nhóm test thông báo, không chạy full suite.

Chrome headless qua Playwright, local cổng 8164, database H2 riêng `notification_ui`. Model mock/disabled, 0 lượt gọi model. Không phải kiểm tra thủ công hoặc điện thoại thật.

| Nội dung kiểm chứng | Kết quả |
|---|---|
| Responsive VI/EN: 360, 390, 768, 1366, 1920px; bell và popup trong viewport | PASS |
| Mở từ mọi tab, liên kết đúng khu xử lý; không thay đổi tài chính | PASS |
| Sự kiện mới cập nhật qua AJAX; không thêm POST ngoài thao tác người dùng | PASS |
| Escape, backdrop, focus trap; cảnh báo payment/Emergency Stop còn hiện | PASS |
| Đã xem qua reload, browser khác tách riêng; không approve draft | PASS |
| Mark all và cập nhật mới trở thành chưa xem | PASS |
| Approval VCB thực qua UI → một receipt; thông báo hoàn tất đầu danh sách, đúng timestamp/plan | PASS |
| Storage bị chặn: giữ trạng thái trong tab, không thay đổi tài chính | PASS |
| Bộ lọc phân biệt cần xử lý/cập nhật; badge không đổi theo bộ lọc; mark all áp dụng toàn danh sách | PASS |
| Click/keyboard trên thông tin đánh dấu đã xem; click tác vụ hoặc mở sources không tự đánh dấu | PASS |
| Nguồn mới hơn lên trên dù ưu tiên thấp; thứ tự/timestamp giữ qua seen, đổi ngôn ngữ và reload | PASS |

Snapshot tài chính trước/sau đọc/lọc bằng nhau. Test receipt dùng approval Sandbox riêng, xác nhận đúng một giao dịch; không dùng thông báo để thực thi.

Bằng chứng: `docs/notification-timeline/` có ảnh desktop/mobile mô phỏng và kết quả Surefire. Các ảnh lịch sử trong docs được giữ nguyên.

## Sau deploy

Chưa kiểm chứng Render hay điện thoại thật cho thay đổi này. Sau khi xác nhận đúng SHA Live, tải lại, mở chuông và kiểm tra bộ lọc/đã xem. Nếu có hóa đơn đã thanh toán, mở thông báo và đối chiếu đúng receipt; không cần thanh toán hoặc reset workspace chung chỉ để kiểm tra UI.
