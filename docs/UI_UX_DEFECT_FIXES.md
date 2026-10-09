# FinBridge — Đóng D1, D2, D3 (local)

Ngày: 05/10/2026. Base HEAD: `2d161f9e0608f83102c387738c86b72e20f358e7`.
Bản được kiểm chứng là working tree local có các sửa bên dưới, chưa commit/push/deploy. Không gọi Gemini; không thao tác Render.

## Thay đổi

| Mục | Sửa | Kết quả |
|---|---|---|
| D1 | Vòng tròn chỉ áp dụng `.payment-step-number`; nhãn `.payment-step-label` co giãn, không bị giới hạn 25×25px. | PASS cả Approval/Delegated, Anh–Việt, 360/390/1366px. |
| D2 | Thông báo có hàng riêng dưới điều khiển header, nằm trong layout. Khi mở chat, chuyển thông báo vào hàng riêng trong panel; đóng chat đưa về header. | PASS không chồng mode/state, nút tiếp tục/approval hoặc input/send. Tự tắt thông báo thành công và đóng lỗi bằng nút vẫn hoạt động. |
| D3 | Dựa vào actionType hiện có để hiển thị chuyển nội bộ/người nhận, mục đích, tài khoản người nhận; hướng dẫn, kiểm tra trước thực thi và hộp xác nhận đúng loại tác vụ. Không hiển thị trường/nhà cung cấp hoặc FX markup cho chuyển nội bộ. | PASS Anh–Việt ở ba kích thước; tuition giữ thông tin trường, hóa đơn và quote. |
| Bằng chứng | Lưu audit trước sửa, JSON, ảnh trước/sau và log test vào `docs/ui-ux-defect-fixes/`. | Không mất khi `mvn clean`. |

Backend tài chính, Policy Guard, Payment Sandbox, schema, dependencies và đường đi AI không thay đổi. Không dùng model để ra kết quả kiểm chứng. Tăng phiên bản asset URL để trình duyệt tải CSS/JS mới.

## File thay đổi

- `src/main/resources/static/app.css`
- `src/main/resources/static/app.js`
- `src/main/resources/templates/home.html`
- Test mới: `src/test/java/com/example/finance/UiCleanupDefectRegressionPlaywrightTest.java`
- Báo cáo này và `docs/ui-ux-defect-fixes/`.

Các file untracked có sẵn như `.agents/`, review của team và ScreenshotEvidenceCollectorTest không thuộc thay đổi này.

## Test thực sự đã chạy

Maven binary đã dùng:
`C:/Users/ADMIN/Downloads/apache-maven-3.9.16-bin/apache-maven-3.9.16/bin/mvn.cmd`.
Tất cả lệnh từ root repository. Playwright Java 1.63 dùng Chrome headless và dependency sẵn có.
DB riêng: `jdbc:h2:mem:ux_cleanup_defects` tại cổng8159; test cleanup cũ dùng `jdbc:h2:mem:ux_cleanup` tại cổng8156. Cả hai đều synthetic.

```powershell
& 'C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd' '-Dtest=UiCleanupDefectRegressionPlaywrightTest,UiCleanupPlaywrightTest' test
```

Kết quả: **8/8 PASS** (3 test mới, 5 cleanup liên quan). [Log](ui-ux-defect-fixes/ui-tests.log).

Sau đó bổ sung viewport ngắn450px và ảnh đầy đủ của hướng dẫn D3; chạy lại đúng hai ca:

```powershell
& 'C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd' '-Dtest=UiCleanupDefectRegressionPlaywrightTest#notificationsUseLayoutSpaceAndNeverCoverPolicyApprovalOrChat+internalTransferReviewUsesRecipientAndTransferGuidanceNotSchoolOrFx' test
```

**2/2 PASS**. [Log](ui-ux-defect-fixes/short-viewport-tests.log).

Sau khi hoàn thiện thêm câu kiểm tra và hộp xác nhận D3, chạy:

```powershell
& 'C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd' '-Dtest=UiCleanupDefectRegressionPlaywrightTest,UiCleanupPlaywrightTest#guidedApprovalAndDelegatedResultKeepModeReceiptAndAuditConsistent' test
```

Lần đầu không compile do fixture gọi nhầm `resume()`; sửa thành `resumeAgent()`.
Lượt chạy tiếp: **3 PASS, 1 ERROR fixture D3**. Fixture tìm nút approval của plan cũ sau Emergency Stop, nhưng plan đó đã bị chặn theo policy; không nới policy để test qua. D1/D2 và guided approval/delegated vẫn PASS. [Log](ui-ux-defect-fixes/final-ui-attempt.log).

Đổi fixture D3 sang tạo plan mới sau resume, chỉ chạy lại ca lỗi:

```powershell
& 'C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd' '-Dtest=UiCleanupDefectRegressionPlaywrightTest#internalTransferReviewUsesRecipientAndTransferGuidanceNotSchoolOrFx' test
```

**1/1 PASS, BUILD SUCCESS**,20,152giây. [Log](ui-ux-defect-fixes/d3-final-tests.log).
Tổng cộng8 test UI khác nhau đã đạt qua các lượt; không cộng các lần chạy lại thành test mới. Trên thay đổi production cuối, D1/D2/guided PASS ở lượt trước, D3 PASS sau sửa fixture; không có thay đổi production giữa hai lượt đó.
Không chạy full suite hoặc audit tài chính đầy đủ. Các test mới xác nhận zero Sandbox execution và zero model classification; test guided cũ kiểm tra riêng tương tác approval/delegated liên quan.

## Bằng chứng ảnh

40 ảnh sau sửa bao gồm từng kích thước và ngôn ngữ trong thư mục `ui-ux-defect-fixes/after`.
Đã mở và xem trực tiếp các ảnh tiêu biểu; đây vẫn là bằng chứng browser automation, không phải thao tác thủ công trên app hoặc điện thoại thật.

- D1,360px VI: [Sau sửa](ui-ux-defect-fixes/after/D1-APPROVAL-360-vi.png).
- D2,390px VI: [Header](ui-ux-defect-fixes/after/D2-header-390-vi.png).
- D2,viewport ngắn360×450 VI: [Chat](ui-ux-defect-fixes/after/D2-chat-short-360-vi.png).
- D3,390px VI: [Hướng dẫn và người nhận](ui-ux-defect-fixes/after/D3-guidance-390-vi.png).
- D2,desktop EN: [Header](ui-ux-defect-fixes/after/D2-header-1366-en.png).
- [Audit trước sửa](ui-ux-defect-fixes/ACCEPTANCE_BEFORE.md), [JSON trước sửa](ui-ux-defect-fixes/before/visual-probe.json).

## Kết luận và giới hạn

**Đóng D1/D2/D3 trên local trong phạm vi đã kiểm chứng.** Không sửa nghiệp vụ, không bắt đầu phase AI.
Chưa kiểm chứng điện thoại thật, bàn phím hệ điều hành/touch, hoặc bản Render sau sửa. Thu nhỏ viewport450px không chứng minh bàn phím thật.
Các tên seed như Emergency Fund vẫn giữ nguyên tên; trường dữ liệu người dùng không bị dịch tùy tiện.

Checklist tối thiểu sau này: kiểm tra chat/bàn phím/đóng mở/cuộn và review trên điện thoại thật; xác nhận SHA Live khi có đợt deploy được cho phép. Không yêu cầu lặp audit tài chính đã đạt.
