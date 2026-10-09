# Header trợ lý gọn hơn

Ngày kiểm chứng: 09/10/2026. Commit nền: `aba465b`.

## Bố cục

- Bỏ dòng FINANCE ASSISTANT và toolbar riêng khỏi panel.
- Header gồm tiêu đề + nút đóng; bên dưới là nhãn môi trường và quyền hạn nhỏ trên cùng dòng. Nhãn quyền hạn đầy đủ vẫn có cho screen reader/tooltip.
- Bỏ form Emergency Stop trong chat. Nút ở header workspace vẫn dùng Policy Guard hiện có, chặn tác vụ thanh toán mới; không phải nút hủy câu trả lời AI.
- Khi PAUSED, chat vẫn hiện “Emergency Stop is active” và cập nhật theo phản hồi backend; không giấu trạng thái an toàn.
- Trên mobile, đóng chat để thao tác Emergency Stop ngoài workspace; shell vẫn inert khi panel mở để giữ focus đúng trong dialog.
- Giữ composer, lịch sử/cuộn, form sinh hoạt, receipt shortcut và thông tin phê duyệt. Không sửa backend, AI, policy, dữ liệu, schema hoặc dependencies.
- Khi visual viewport rất thấp, giảm padding header và giới hạn vùng thông báo có cuộn để form/ô chat không bị đẩy khỏi màn hình.

## Kiểm chứng

Chrome headless qua Playwright, ứng dụng Spring Boot local, H2 test riêng; AI mock. Không gọi Gemini hay thao tác Render. Đây không phải kiểm tra thủ công/điện thoại thật.

Lượt đầu:

```powershell
mvn '-Dtest=AssistantPanelPlaywrightTest#compactHeaderKeepsChatSpaceAndGlobalStopUpdatesThePausedStatus+mobilePanelFitsLanguageAndKeyboardWorkWithoutExposingDemoControls+emergencyStopBlocksLateDraftAndResetDiscardsLateResponseAndRestoresComposer+reopeningLongChatRestoresTheLastReadPositionAndDraftAcrossTabs+receiptShortcutStaysVisibleAndOpensTheDisplayedReceiptWithoutExecution,UiReviewFollowupPlaywrightTest#mobilePanelTrapsFocusAndDesktopRemainsNonmodal+scopeSourcesAndNewCopyStayBilingualAndUserAuthoredTextStaysVerbatim,AssistantAnswerPresentationPlaywrightTest#runwayChunksStayOneAnswerAndFormAndComposerFitReducedVisualViewport' test
```

8 ca: 7 PASS, 1 FAIL. Ca visual viewport 337px phát hiện form/notice đẩy composer ra khỏi panel. Đã chỉnh CSS chế độ màn hình thấp, giữ nguyên assertion kiểm tra nút thực sự nằm trong viewport và không bị che.

Chạy lại ca lỗi và hai ca header/mobile liên quan:

```powershell
mvn '-Dtest=AssistantAnswerPresentationPlaywrightTest#runwayChunksStayOneAnswerAndFormAndComposerFitReducedVisualViewport,AssistantPanelPlaywrightTest#mobilePanelFitsLanguageAndKeyboardWorkWithoutExposingDemoControls+compactHeaderKeepsChatSpaceAndGlobalStopUpdatesThePausedStatus' test
```

3/3 PASS, BUILD SUCCESS. **8 ca khác nhau có kết quả PASS qua hai lượt**, không phải full suite chạy lại.

Bao phủ:

- VI/EN tại 360, 390, 1440px: header ACTIVE cao không quá 96px, không tràn; hội thoại giữ ít nhất 55% panel khi không có form/notice; nút đóng và input nằm trong viewport.
- Emergency Stop toàn cục → chat hiện PAUSED → Resume → trạng thái dừng biến mất; không tạo giao dịch.
- Dừng trong lúc model mock đang trả draft/reset chặn phản hồi cũ, không tạo payment.
- Đóng/mở lịch sử chat dài giữ vị trí và draft; receipt shortcut mở đúng receipt, không thực thi thêm.
- Mobile focus trap, desktop nonmodal; bản dịch quyền hạn và tooltip nút global, nội dung người dùng giữ nguyên.
- Form runway + input/send được kiểm tra hit-test ở visual viewport giả lập 430px và 337px. Không thay phép tính hoặc dữ liệu tài chính.

Maven thực thi: `C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd`.

## Bằng chứng và giới hạn

Ảnh/log đầy đủ lưu trong `docs/chat-header/`. Bằng chứng cũ trong `docs/ui-ux-followup/` được giữ nguyên.

Chưa nghiệm thu Render sau deploy hoặc bàn phím điện thoại thật. Sau SHA Live, tải lại và mở chat: tiêu đề/nhãn gọn, không còn nút stop trong panel. Để dùng Emergency Stop trên mobile, đóng chat và dùng nút chung ở đầu workspace; không thử dừng/reset Render khi chưa phối hợp team.
