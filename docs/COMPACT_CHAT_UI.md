# Thu gọn giao diện trợ lý — 09/10/2026

## Thay đổi

- Bỏ nhãn màn hình đang xem, mô tả ngữ cảnh, nhãn chủ đề và hai nhóm câu hỏi nhanh khỏi panel chat.
- Giữ ngữ cảnh phía server; giao diện chỉ hiển thị lựa chọn làm rõ khi thực sự cần chọn tài khoản, hóa đơn hoặc chủ đề.
- Giữ thông tin kế hoạch thu gọn, form sinh hoạt, shortcut xem kế hoạch/biên nhận, composer và cảnh báo quyền thanh toán.
- Đồng bộ toàn bộ node lựa chọn làm rõ khi nhận phản hồi để cập nhật cả trạng thái ẩn/hiện, không chỉ nội dung.
- Không thay đổi Java nghiệp vụ, Policy Guard, AI routing, schema, configuration hoặc dependencies.

## Ý nghĩa của đã xem

Đánh dấu đã xem (nếu bổ sung sau) chỉ ghi nhận việc đọc thông báo; không phê duyệt, không hoàn tất tác vụ. Kế hoạch chưa duyệt vẫn cần duyệt trên màn hình thanh toán. Phiên bản này chưa bổ sung nút hay lưu trạng thái đã xem; badge vẫn đếm các mục cần chú ý hiện tại.

## Kiểm chứng thực tế

Chrome được điều khiển bằng Playwright trên ứng dụng local, H2 test riêng, LLM mock; không gọi Gemini và không thao tác Render. Đây là browser automation, không phải kiểm tra thủ công hoặc điện thoại thật.

Lệnh đầu tiên:

```powershell
mvn '-Dtest=AssistantPanelPlaywrightTest,ContextualConversationPlaywrightTest,GlobalAssistantPlaywrightTest#pendingAccountChoiceIsVisibleAndBoundOnlyToThisSession' test
```

15 ca: 11 đạt, 3 fail, 1 error. 9 ca AssistantPanel đều đạt. Các lỗi còn lại xác định node context giữ thuộc tính hidden/topic cũ sau AJAX dù nội dung lựa chọn đã cập nhật.

Sau sửa đồng bộ hiển thị, chạy lại đúng 4 ca lỗi:

```powershell
mvn '-Dtest=ContextualConversationPlaywrightTest#targetedChoicesAndSessionIsolationWorkThroughTheSharedPanel+contextualEntryPointsBindTheExactBillAndPlanAndDoNotPay+contextSwitchAndResetWhileModelWaitsCannotRestoreOldTopicOrCreateDraft,GlobalAssistantPlaywrightTest#pendingAccountChoiceIsVisibleAndBoundOnlyToThisSession' test
```

4/4 đạt, BUILD SUCCESS. Tổng cộng 15 ca khác nhau có kết quả đạt qua hai lượt; không chạy full suite.

Bao phủ: không gọi model khi chỉ mở/đổi màn hình; giữ draft và trạng thái chờ; chống gửi trùng; phản hồi lỗi mạng; layout mobile mô phỏng; shortcut plan/receipt; lựa chọn làm rõ; lựa chọn bill/plan chính xác; tách phiên; reset/context-switch khi request chờ; không tạo thanh toán từ câu hỏi chỉ đọc.

Maven thực thi: `C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd`.

## Bằng chứng và giới hạn

- Log đầy đủ: `compact-chat/logs/compact-chat-verification.log`, `compact-chat/logs/compact-chat-recheck.log`.
- Ảnh: `compact-chat/desktop-vi.png`, `compact-chat/mobile-simulated-vi.png`, `compact-chat/plan-desktop-en.png`, `compact-chat/clarification-vi.png`.
- Ảnh mock có thể giữ nguyên ngôn ngữ của phản hồi fixture; đợt này không sửa sinh câu trả lời hay dịch nội dung backend.
- Chưa kiểm chứng điện thoại thật hoặc bản Render sau deploy.
