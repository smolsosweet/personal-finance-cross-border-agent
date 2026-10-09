# Chọn kênh và làm gọn chatbot — 09/10/2026

Base: `610369d`. Đợt này chỉ sửa UI/UX, không sửa financial service, eligibility, policy, schema hoặc dependency.

## Thay đổi

- Click nội dung thẻ kênh hoặc nút Chọn để so sánh để highlight; nút hỗ trợ Enter/Space. Chỉ một thẻ được chọn, lựa chọn giữ theo hóa đơn trong sessionStorage của tab trình duyệt.
- Chọn kênh chỉ đánh dấu phương án để so sánh; chưa tạo draft, chưa đổi context tài chính trong chat hay thực thi. Nút Tạo kế hoạch của kênh nào vẫn tạo draft gắn với chính kênh/tài khoản/quote đó.
- Kênh không hỗ trợ hành lang không thể chọn hoặc tạo plan. Kênh thiếu tiền/báo giá hết hạn vẫn có thể xem/đánh dấu so sánh; các điều kiện tạo plan hiện có giữ nguyên.
- Bỏ dropdown chuyển tab và VI/EN trong chatbot. Đổi ngôn ngữ qua nút chính website; quyền hạn và Emergency Stop trong chat giữ nguyên. Cập nhật hook JS đã dùng control bị xóa.
- Kênh đã liên kết nhưng không đủ eligibility có thông báo rõ hành lang quốc gia/tiền tệ, không phải thiếu liên kết; các con số của kênh bị chặn có nhãn chỉ tham khảo.
- Ví dụ MoMo với hóa đơn USD: tài khoản đã liên kết, nhưng demo chưa cấu hình hỗ trợ Vietnam → United States, VND → USD. Không liên kết lại để vượt điều kiện này. Bank B vẫn là trường hợp chưa kết nối riêng biệt.
- Asset version: `20261009-channel-chat`.

## Kiểm chứng thực sự chạy

Chrome headless, Playwright Java, H2 local riêng. Không Gemini thật, không Render, không điện thoại thật. Chạy test mới và test panel/xuyên tab liên quan, không full suite.

```powershell
mvn "-Dtest=StudentExpensePlaywrightE2ETest#channelSelectionHighlightsOnlyOneCardWithoutFinancialMutationAndSurvivesRefresh+connectedMomoUsdRouteIsClearlyReferenceOnlyAndCannotBeSelected,AssistantPanelPlaywrightTest,GlobalAssistantPlaywrightTest" test
mvn "-Dtest=UiReviewFollowupPlaywrightTest#mobilePanelTrapsFocusAndDesktopRemainsNonmodal,UiProductRefinementPlaywrightTest#bilingualLabelsUserTextDialogsAndMobileTargetsStayUsable" test
mvn "-Dtest=AssistantPanelPlaywrightTest#mobilePanelFitsLanguageAndKeyboardWorkWithoutExposingDemoControls,UiReviewFollowupPlaywrightTest#mobilePanelTrapsFocusAndDesktopRemainsNonmodal" test
```

Kết quả sau sửa: **18 ca riêng biệt đạt** qua các lượt chạy và recheck có mục tiêu:

| Nhóm | Đạt |
|---|---:|
| Hai ca kênh/chat mới | 2/2 |
| AssistantPanelPlaywrightTest | 9/9 |
| GlobalAssistantPlaywrightTest | 5/5 |
| Hai ca focus/ngôn ngữ/điều hướng UI | 2/2 |

Lượt đầu bắt được hook còn dùng language group đã xóa; đã bỏ hook. Lượt sau còn hai kỳ vọng test cũ: focus phải về nút launcher vừa dùng mở lại, và mobile phải bấm tab đang hiển thị thay vì sidebar ẩn. Đã cập nhật theo thao tác người dùng và recheck 2/2 BUILD SUCCESS. Không coi các lượt thất bại trước sửa là PASS.

Các test dùng nút language cũ được chuyển sang helper đóng chat → đổi ngôn ngữ chính → mở lại; test nguồn đó vẫn compile nhưng chỉ các nhóm trong bảng được chạy trong đợt này.

Log: `docs/channel-chat-ui/`. Ảnh panel mobile: `mobile-chat-vi.png`, `mobile-focus.png`.

## Test nhanh sau khi Render chạy đúng commit mới

1. Ctrl+F5, mở Tài chính du học, click Bank A rồi Vietcombank. Viền và nhãn Đang chọn chuyển sang đúng một thẻ; chưa có draft/thanh toán mới.
2. Bấm Chi tiết; dialog mở mà không đổi kênh đang chọn. Đổi ưu tiên/làm mới quote hoặc F5, highlight còn đúng kênh cho cùng hóa đơn.
3. Chọn hóa đơn USD synthetic; MoMo hiển thị Tài khoản đã liên kết, hành lang VND → USD chưa hỗ trợ; nút chọn bị khóa, không có Tạo kế hoạch; số liệu chỉ tham khảo.
4. Mở chat: không còn dropdown tab/VIEN; quyền hạn và Dừng khẩn cấp vẫn dễ thấy.
5. Đóng chat → đổi VI/EN ngoài website → mở lại. Câu đang nhập và lịch sử giữ nguyên; câu trả lời cũ giữ ngôn ngữ của lượt đó.
6. Thử desktop và mobile: ô nhập/nút Gửi hiển thị, Escape/Đóng trả focus về nút vừa mở chat. Điện thoại thật vẫn cần kiểm chứng riêng.

Không cần reset/thanh toán để thử các thay đổi này. Nếu muốn thử tạo plan/Sandbox, phải phối hợp team như checklist nghiệm thu hiện có.
