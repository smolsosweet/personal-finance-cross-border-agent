# Sửa cuộn danh sách và cân bố cục — 05/10/2026

## Nguyên nhân

1. Danh sách giữ height cố định và luôn tạo nút Scroll down. Khi nội dung vừa đủ, nút bị disabled nhưng vẫn hiện, tạo khoảng trống dư và cảm giác điều khiển hỏng.
2. CSS mobile `#transactions .table-scroll{overflow:visible}` có độ ưu tiên cao hơn `.bounded-list`, khiến lịch sử giao dịch không cuộn bên trong dù bị giới hạn chiều cao. Regression browser bổ sung đã tái hiện: scrollHeight lớn hơn clientHeight nhưng mouse wheel không làm scrollTop tăng.
3. Attention Center span hai hàng, còn các thẻ cạnh bên xếp một hàng, gây khối cao/ngắn không đồng đều.

## Thay đổi

- Dùng max-height thay vì height: danh sách ngắn thu gọn; danh sách dài vẫn bị giới hạn và có cuộn.
- Chỉ hiện Scroll down/Cuộn xuống khi thật sự có nội dung ngoài vùng nhìn thấy. Cập nhật sau chuyển tab, thay nội dung, đổi kích thước và lọc phần tử.
- Cho phép cuộn trang tiếp khi tới mép vùng cuộn, tránh cảm giác chuột bị giữ trong hộp.
- Quy tắc overflow của lịch sử có độ ưu tiên đúng trên mobile; giữ cuộn ngang cho bảng desktop.
- Thẻ tài khoản trong cùng hàng bằng chiều cao; bố trí tên, thông tin nguồn, số dư và footer nhất quán; chữ mô tả 12px, thời điểm 11px, số dư 24px.
- Attention Center và Proactive Feed nằm cùng một hàng cân chiều cao trên desktop; cash flow nằm hàng riêng toàn chiều rộng. Mobile xếp một cột.
- Summary, cảnh báo và approval không bị ép chiều cao/cắt nội dung. Cache version UI đổi sang 20261005-scroll-layout.

Chỉ sửa app.css, app.js, home.html. Không sửa backend tài chính, provider, policy, schema, dependencies hoặc dữ liệu Render.

## Kiểm chứng thực tế

```powershell
mvn -Dtest=WorkspaceListScrollPlaywrightTest,UnifiedAccountsPlaywrightTest,FinanceOverviewPlaywrightE2ETest,UiReviewFollowupPlaywrightTest test
```

Lượt cuối: **13 test PASS, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS, 37,547 giây. Maven thực tế gọi bằng `C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd`; output tại target/scroll-layout-final.log.

Không cộng số lần rerun thành tổng test. Trong quá trình bổ sung coverage: có một lỗi selector chọn tab desktop ẩn trên mobile, sau đó phát hiện lỗi overflow thật của lịch sử; cả hai được giải quyết và các ca đó thực sự chạy PASS ở lượt cuối.

5 ca mới trong WorkspaceListScrollPlaywrightTest:
- Danh sách ngắn không có nút vô dụng/khoảng trống cố định; các thẻ seed cân chiều cao.
- Danh sách 21 nguồn cuộn bằng wheel, button và End ở viewport 1594, 768, 390; nút không làm trang nhảy.
- Điều khiển tự cập nhật khi thêm/xóa phần tử và chuyển view ẩn/hiện.
- Lịch sử giao dịch mobile cuộn bằng wheel và nút.
- Hai khối Overview ngang hàng desktop Việt–Anh, xếp cột mobile; snapshot tài chính không đổi.

8 ca còn lại kiểm tra các phần UI liên quan: tài khoản, Overview, payment update/history/audit và mobile navigation/dialog/pagination. Chỉ chạy nhóm liên quan; không chạy lại full suite/backend không liên quan. Test dùng synthetic H2 và mock model; không gọi Gemini/Ollama thật. Không bỏ/skip test để đạt kết quả.

## Bằng chứng và giới hạn

- docs/scroll-layout/test-results.json: kết quả 4 class thực sự chạy, không chứa system properties/key.
- docs/scroll-layout/accounts-seed-desktop.png: thẻ seed và nút cuộn.
- docs/scroll-layout/accounts-long-mobile.png: danh sách dài trên mobile giả lập.
- docs/scroll-layout/overview-mobile.png: bố cục Overview mobile.

Đã xem ảnh desktop/mobile. Chưa kiểm tra điện thoại thật hoặc bản Render mới. Sau deploy cần xác nhận SHA Live rồi tải lại trang (Ctrl+F5 nếu cache cũ); thử wheel và nút ở Tài khoản & tiền mặt, lịch sử giao dịch. Danh sách đã hiện hết sẽ không có nút cuộn; tới cuối danh sách thì nút không cuộn tiếp xuống.
