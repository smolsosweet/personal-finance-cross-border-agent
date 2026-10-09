# Nhãn quyền hạn, dừng thanh toán và hủy kế hoạch

Ngày: 09/10/2026. Commit nền: `7b4e92b`.

- Header: **Thanh toán cần phê duyệt** khi ở APPROVAL; **Ủy quyền tác vụ rủi ro thấp** khi ở DELEGATED. Tooltip nhắc thanh toán giáo dục luôn cần phê duyệt.
- Nút toàn cục: **Dừng mọi thanh toán mới**. Tooltip giải thích không hoàn tác khoản đã hoàn tất. Khi dừng, có **Tiếp tục thanh toán** và cảnh báo PAUSED hiện có.
- Kế hoạch: **Hủy kế hoạch**, có hướng dẫn liên kết bằng `aria-describedby`: chỉ hủy kế hoạch này, không ảnh hưởng các tác vụ khác.
- Trong phần thu gọn **Thiết lập demo và công cụ môi trường**, có **Cấu hình demo: quyền hạn tác vụ rủi ro thấp**. Mô tả đúng khả năng hiện có: ủy quyền chuyển vào Quỹ khẩn cấp đã được cho phép, trong hạn mức; học phí vẫn cần phê duyệt riêng. Đổi mode không tự thực thi thanh toán. Nút mode có trạng thái `aria-pressed`.
- Việt–Anh và cách xuống dòng trên header màn hình nhỏ đã được kiểm chứng. Badge trong chat giữ cách hiển thị ngắn hiện có.

Chỉ sửa template, CSS, bản dịch và test UI liên quan. Không sửa backend tài chính, Policy Guard, schema, secrets hoặc hosting. Không thao tác Render.

## Test thực chạy

Maven 3.9.16, Chrome headless qua Playwright; H2 riêng `payment_workflow_e2e` (cổng 8095) và `ux_cleanup` (cổng 8156), synthetic và Sandbox. Model mock; không gọi Gemini. Đây là browser automation, không phải kiểm tra điện thoại thật.

```powershell
mvn '-Dtest=UiCleanupPlaywrightTest#modeAndSimulationRemainVisibleAcrossViewportAndLanguageChanges+blockedReasonRetainsBackendEvidenceAndPauseIsGlobal+guidedApprovalAndDelegatedResultKeepModeReceiptAndAuditConsistent,PaymentWorkflowPlaywrightE2ETest#nextBillPlanAndCancellationNeverDisplayAnotherActionsReceipt+emergencyStopIsAccessibleWithDemoToolsClosedAndBlocksPaymentUntilRecovery+mobileVietnameseAlipayReviewRemainsApprovalOnlyInDelegatedMode' test
```

**6/6 PASS**, Failures 0, Errors 0, Skipped 0, BUILD SUCCESS; Maven 38,464 giây. Phạm vi: nhãn/state qua 360/390/768/1024/1366px và VI/EN; pause/resume; chỉ hủy plan đang xem và giữ receipt cũ; học phí vẫn chờ duyệt khi mode ủy quyền; approval tạo receipt; chuyển nội bộ được ủy quyền và bị chặn khi pause.

Sau khi thêm assertion Việt–Anh cho phần thiết lập và xác nhận đổi mode không phát sinh thanh toán, chỉ chạy lại ca vừa bổ sung:

```powershell
mvn '-Dtest=UiCleanupPlaywrightTest#guidedApprovalAndDelegatedResultKeepModeReceiptAndAuditConsistent' test
```

**1/1 PASS**, BUILD SUCCESS; Maven 21,754 giây. Tổng 6 ca khác nhau đã đạt, 7 lần thực thi. Không chạy full suite.

Ảnh và logs: `docs/payment-control-ux/`. Chưa kiểm chứng Render và điện thoại thật. Sau deploy, xác nhận SHA Live, tải lại và kiểm tra nhãn bằng VI/EN. Các thao tác dừng/thanh toán trên Render dùng chung vẫn cần phối hợp team.
