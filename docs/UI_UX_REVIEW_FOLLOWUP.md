# Kiểm chứng và sửa UX theo team review

## Phạm vi
Baseline: `707e4c2`. Review Antigravity ghi `5065543` và có nội dung được quan sát trong lúc UI đang thay đổi; vì vậy các phát hiện được tái hiện trên bản hiện tại trước khi sửa.

Chỉ sửa `home.html`, `app.css`, `app.js`, thêm test/bằng chứng. Không đổi Java main, business rule, schema, cấu hình, dependency, API/provider. Không push/deploy hoặc dùng Gemini/Ollama thật. Không sửa báo cáo/artifact Antigravity.

## Tái hiện trước sửa
Lệnh chạy:
```powershell
& 'C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd' '-Dtest=UiReviewFollowupPlaywrightTest' '-Dux.review.stage=before' test *> target/ux-review-before.log
```

4 ca: **1 PASS, 3 FAIL**:
- Thanh tab sau cuộn Việt–Anh ở 360/390/768/1440: PASS.
- Lỗi overlay xác nhận ở 360px: thêm đủ hóa đơn synthetic để có trang 2, cuộn Next gần đáy, `elementFromPoint` tại giữa nút trả về launcher; Next không nhận tap. Ảnh `before/student-bill-360.png`.
- Panel mobile vẫn `aria-modal=false`, chưa có vòng focus riêng; FAIL đối với yêu cầu focus khi panel gần toàn màn hình.
- Ca nguồn/scope FAIL vì chưa có nhãn nguồn lập kế hoạch mới. Đây là tiêu chí copy được bổ sung, không phải lỗi số liệu.

## Thay đổi
1. Mobile: nút mở trợ lý 44px nằm trong header luôn hiển thị, không nổi đè lên nội dung. Desktop giữ nút nổi; phân trang có vùng trống bên phải.
2. Mobile panel: đúng modal semantics, nền `inert`, backdrop, vòng Tab/Shift+Tab, Escape đóng, trả focus. Desktop vẫn nonmodal để tương tác giữa trang và trợ lý.
3. Resize desktop → mobile đưa focus từ trang vào panel; mobile → desktop khôi phục nền. Giữ draft, scroll, pending/duplicate/timeout/stale handling.
4. Nguồn lập kế hoạch có tiêu đề và mô tả riêng; không gộp với tài khoản thanh toán hoặc đổi số liệu.
5. Lịch sử ghi rõ “Tất cả thời gian”; bộ lọc thu hẹp lịch sử, không đổi báo cáo tháng.
6. Việt–Anh cho nội dung mới, tooltip Dừng khẩn cấp giải thích chặn thanh toán mới và không hoàn tác khoản đã hoàn tất. Nút vẫn chặn ngay, không thêm confirmation.
7. Skip link tới nội dung chính. Giữ dialog native và focus restoration hiện có.

## Kết quả sau sửa
Lượt đầu: 3/4 PASS, một expectation chữ thường “bộ lọc” thay vì nhãn “Bộ lọc”; sửa expectation đúng literal, không giảm assertion hành vi.

Lệnh hồi quy có mục tiêu:
```powershell
& 'C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd' '-Dtest=UiReviewFollowupPlaywrightTest,AssistantPanelPlaywrightTest,AssistantAnswerPresentationPlaywrightTest,GlobalAssistantPlaywrightTest,PaymentWorkflowPlaywrightE2ETest,ContextualConversationPlaywrightTest,RunwayPlaywrightTest,FinanceOverviewPlaywrightE2ETest,UiProductRefinementPlaywrightTest#stateSpecificReviewAndReceiptRemainAuthoritative+expiredUnavailableInvalidatedAndEmergencyStatesKeepRecoveryVisible+balancesBudgetsAndTransactionAmountsKeepSourcesAndCurrenciesDistinct' test *> target/ux-review-related.log
```

Kết quả cuối: **42 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**. Hoàn tất 05/10/2026 11:58:46 UTC+7, **2 phút 01 giây**.

| Lớp đã chạy | Số ca đạt |
|---|---:|
| UiReviewFollowupPlaywrightTest (mới) | 4 |
| AssistantPanelPlaywrightTest | 9 |
| AssistantAnswerPresentationPlaywrightTest | 7 |
| ContextualConversationPlaywrightTest | 5 |
| FinanceOverviewPlaywrightE2ETest | 1 |
| GlobalAssistantPlaywrightTest | 5 |
| PaymentWorkflowPlaywrightE2ETest | 5 |
| RunwayPlaywrightTest | 3 |
| UiProductRefinementPlaywrightTest (3 method liên quan) | 3 |

Tất cả 42 ca là kiểm chứng browser trên app local/database test; provider mock hoặc tắt. **Số request Gemini/Ollama thật: 0.**

## Phạm vi bằng chứng
- Chrome headless thật, Spring Boot thật, H2 test riêng; intent provider mock.
- Tái hiện hit target tại 20/50/80% chiều ngang của Next; bấm thực tế vào trang 2, không mở chat. Hai danh sách, 4 chiều rộng.
- Thanh tab không bị header che sau cuộn; kiểm tra click thực tế Việt–Anh tại 4 chiều rộng.
- 32 bước Tab/Shift+Tab trong mobile panel; chuyển 390×430 → 1440×900 → 360×844, Escape và chuyển màn hình trong chat.
- Bản nháp và tên hóa đơn có từ trùng label vẫn nguyên văn.
- Hồi quy liên quan: chat xuyên tab/phiên, draft B giữ workspace A, chưa duyệt không có payment/receipt, duyệt đúng kế hoạch một lần, quote/Bank B/vô hiệu/Emergency Stop, edit/clear/reset runway, lỗi request và stale response.
- Không chạy toàn bộ suite không liên quan; không gọi model thật.

## Ảnh
- `docs/ui-ux-followup/before/`: 2 ảnh trước sửa.
- `docs/ui-ux-followup/after/`: 9 ảnh sau sửa: Next hóa đơn/giao dịch tại 360/390/768/1440, panel focus ở 390×844.
- Ảnh lỗi và sau sửa cùng vị trí cuộn, không chỉ chụp đầu trang.

## File của task
- `src/main/resources/templates/home.html`
- `src/main/resources/static/app.css`
- `src/main/resources/static/app.js`
- `src/test/java/com/example/finance/UiReviewFollowupPlaywrightTest.java`
- Báo cáo này và 11 PNG trong `docs/ui-ux-followup/`.

## Giới hạn
- Đây là kiểm chứng Chrome desktop/mobile emulation; chưa kiểm tra điện thoại thật, Safari hoặc screen reader.
- Không chứng nhận WCAG toàn diện từ các ca keyboard này.
- Vẫn synthetic/shared workspace, planner và Sandbox tách nguồn; không phải production tiền thật.
- App local đang chạy của người dùng và Render không được restart/thay đổi. Cần restart local để xem bản mới.
- .agents/, UI_UX_TEAM_REVIEW.md, ui-ux-team-review/ và ScreenshotEvidenceCollectorTest.java từ Antigravity không thuộc commit.

## Ranh giới và kết luận
**35/35 file được bảo vệ giống SHA-256 baseline**: toàn bộ Java main (kể cả formatter), resource cấu hình/schema và pom. Diff chỉ ở UI/static/template, test và báo cáo/ảnh. API endpoints, payload tài chính, approval/Policy Guard/execution và object resolution không thay đổi.

`git diff --check`: PASS.

**Kết luận: PASS cho đợt sửa UX có mục tiêu này.** Lỗi che Next đã tái hiện và sửa; vòng focus mobile được kiểm chứng; các cải thiện copy/scope đạt Việt–Anh; hồi quy các luồng liên quan đạt. Không coi đây là chứng nhận production/WCAG hoặc nghiệm thu điện thoại thật.

## Đối chiếu các nhận xét
| Nhận xét | Kết quả |
|---|---|
| Header che tab mobile | Không tái hiện trên baseline 707e4c2; kiểm tra cuộn/hit target Việt–Anh tại 4 chiều rộng đạt |
| Launcher che Next | Tái hiện tại 360px; sau sửa hit target và click Next đạt ở 4 chiều rộng cho cả hai danh sách |
| Focus mobile đi vào nền | Bổ sung modal semantics/inert/Tab cycle; 32 bước Tab/Shift+Tab và resize/đóng/mở đạt |
| Dịch bằng thay từ | Cơ chế đã bỏ ở 707e4c2; đợt này chỉ thêm exact labels và bản dịch title mới; tên hóa đơn/bản nháp nguyên văn |
| Emergency Stop khó hiểu | Thêm tooltip Việt–Anh; giữ vị trí dễ tìm và không thêm xác nhận trì hoãn |
| Hai nguồn số dư dễ nhầm | Thêm tiêu đề/phân nhóm và giải thích không gồm tài khoản thanh toán, giữ nguyên số liệu |
| Phạm vi lịch sử không rõ | Hiện All time/Tất cả thời gian và giải thích bộ lọc khác kỳ báo cáo tháng |

Ảnh before/after đã được mở xem trực quan ở 360px và panel 390px. Các artifact Antigravity được giữ nguyên ngoài commit.
