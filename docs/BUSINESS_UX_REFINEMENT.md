# FinBridge — hoàn thiện UX nghiệp vụ và kiểm chứng local

Ngày: 09/10/2026. Phiên bản nền: `db454174209e29b2c9e13a783f9c27957546e28c`.

## 1. Phạm vi và kết quả

Đợt này cải thiện cách hiển thị và thao tác của bản FinBridge hiện có. Không thay đổi Java nghiệp vụ, Policy Guard, Account Ledger, AI/provider, schema, cấu hình hoặc dependencies. Không dùng Render, API Gemini hay tiền thật. Những cải tiến dưới đây không biến Payment Sandbox thành dịch vụ ngân hàng production.

- Tổng quan gộp tổng số dư VND, tiền giữ trước 30 ngày, đệm an toàn và phần còn lại vào một khối. Danh sách từng tài khoản dùng cùng dữ liệu cũ; không cộng tiền tệ khác vào VND. Các mục tóm tắt không còn lặp lại phía dưới.
- Thẻ tài khoản đều nhau; desktop ba cột, màn hình nhỏ hai cột. Danh sách dài có vùng cuộn; lịch sử giao dịch giữ phân trang hiện có.
- Hóa đơn và kênh so sánh có nút xem các mục trước/tiếp theo khi nội dung vượt chiều ngang. Chọn kênh vẫn chỉ highlight lựa chọn; không tự tạo plan hoặc thanh toán.
- Phê duyệt mở hộp xác nhận trong ứng dụng thay cho `window.confirm`: đúng hóa đơn/tham chiếu, tài khoản nguồn, người thụ hưởng, kênh, tiền nguồn, phí, tổng tiền trừ và số dư dự kiến do backend cung cấp. Tích xác nhận thông tin rồi bấm **Xác nhận và thanh toán** mới gửi yêu cầu approve.
- Nút đóng nhận focus đầu tiên; Escape/Quay lại không phê duyệt; focus trở lại nút mở. Vùng thông tin cuộn riêng; các nút hành động, công bố mô phỏng và trạng thái báo giá ở phần cố định.
- Báo giá hết hạn trong khi popup mở làm nút xác nhận bị vô hiệu. Chuyển nội bộ không bị yêu cầu báo giá FX. Backend vẫn kiểm tra chính sách tại thời điểm thực thi.
- Emergency Stop vẫn dễ truy cập trên header và trong popup; giảm màu nền đỏ trên header, không thêm bước cản trở thao tác dừng.
- Giữ lời giải thích chặn từ backend; mã kỹ thuật thu gọn trong “Thông tin kỹ thuật”. Dịch các nhãn mới Anh–Việt và giữ nguyên tên/mã người dùng nhập.
- Giảm công bố môi trường lặp lại ở sidebar/footer; giữ nhãn mô phỏng ở header, cảnh báo workspace chung và công bố tại xác nhận/receipt.

## 2. Đối chiếu nhận xét review

Một số nhận xét trong báo cáo bên ngoài không phù hợp phiên bản code được kiểm tra:

- Đệm an toàn hiện là **3.000.000 VND**, không phải 5.000.000; không thay đổi policy theo số trong review.
- Tổng quan và Sandbox đã dùng Account Ledger chung. Hai khối hiển thị không chứng minh có hai nguồn tiền độc lập; lần này xử lý sự lặp trong giao diện.
- Chat đã có quản lý focus. Không cần viết lại AI/chat hay thay kiến trúc chỉ để giải quyết review này.
- `window.confirm` là điểm cần cải thiện UX; kết quả đọc code không chứng minh nó bỏ qua backend approval hoặc policy.
- Không giả lập PIN để tạo cảm giác đã có xác thực giao dịch thật. Không đổi tên thực thể synthetic thành trường/ngân hàng “chính thức”. Không ẩn Emergency Stop trong cài đặt.

## 3. Môi trường kiểm chứng

Windows, Java 21, Maven 3.9.16, Playwright Java và Chrome headless đã có trong repository. Spring Boot khởi động thực trên local; từng lớp test dùng H2 trong bộ nhớ riêng. Test mới dùng cổng 8163 và database `business_ux`.

Đây là **browser automation**, không phải thao tác thủ công hay điện thoại thật. Viewport: 360, 390, 768, 1366, 1440, 1920px; popup còn kiểm tra 360×430 và 390×600. Hai ngôn ngữ VI/EN. Số request sinh nội dung Gemini: **0**. Model được mock/disable trong những ca chat hồi quy; không tuyên bố đã kiểm chứng model thật trong đợt này.

## 4. Lệnh đã chạy và kết quả thực tế

Lệnh dưới dùng `mvn`; trên máy này được thực thi bằng đường dẫn Maven đầy đủ:
`C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd`.

### 4.1 Test mới và vòng sửa lỗi

```powershell
mvn '-Dtest=BusinessUxPlaywrightTest' test
mvn '-Dtest=BusinessUxPlaywrightTest#overviewHasOneVndSummaryAndBalancedCardsAcrossViewports' test
```

Các lượt đầu được giữ trong log, không tính là PASS: lỗi compile do test gọi API private; sau đó ba assertion phát hiện chiều cao thẻ không đều, focus và việc chuyển nội bộ bị đòi quote. Đã sửa đúng tầng UI/test. Một lượt tiếp theo còn thất bại ở grid mobile; sửa `grid-auto-rows` rồi kiểm chứng tiếp.

### 4.2 Hồi quy liên quan

```powershell
mvn '-Dtest=BusinessUxPlaywrightTest,UiReviewFollowupPlaywrightTest,UiCleanupPlaywrightTest,UiProductRefinementPlaywrightTest,PaymentWorkflowPlaywrightE2ETest,UnifiedAccountsPlaywrightTest,StudentExpensePlaywrightE2ETest#channelSelectionHighlightsOnlyOneCardWithoutFinancialMutationAndSurvivesRefresh+connectedMomoUsdRouteIsClearlyReferenceOnlyAndCannotBeSelected+inPlaceFormsKeepApprovalAndSandboxExecutionControlledAndEmergencyStopBlocksNewActions,FinBridgePlaywrightE2ETest#recipientMismatchAndExpiredQuoteAreBlockedInBrowserFlow+approvedPaymentCreatesMultiCurrencyReceiptAndAuditTrail,GlobalAssistantPlaywrightTest#completedTuitionAnswersUseReceiptAndCurrentBalanceFromAnyTab' test
```

**35 test: 33 PASS, 2 FAIL.** Hai failure là expectation cũ về tiêu đề Tổng quan và phạm vi giao dịch. Selector GlobalAssistant ở lệnh này không tồn tại nên **không chạy ca GlobalAssistant nào**; không dùng nó làm bằng chứng cho chat.

```powershell
mvn '-Dtest=BusinessUxPlaywrightTest,UiProductRefinementPlaywrightTest#bilingualLabelsUserTextDialogsAndMobileTargetsStayUsable,UiReviewFollowupPlaywrightTest#scopeSourcesAndNewCopyStayBilingualAndUserAuthoredTextStaysVerbatim,GlobalAssistantPlaywrightTest#historicalAndCurrentBalancesRemainDifferentAcrossAllTabsAfterActualPayment+draftBFromChatReviewsAndPaysBOnlyWhileAStaysSelected,UiCleanupDefectRegressionPlaywrightTest' test
```

**14 test: 12 PASS, 1 FAIL, 1 ERROR.** Hai ca GlobalAssistant thực sự chạy và đạt. Lỗi còn lại: nhãn VI của “All time” khác expectation; test chuyển nội bộ vẫn đọc attribute của `window.confirm` đã được thay bằng dialog.

```powershell
mvn '-Dtest=BusinessUxPlaywrightTest,UiReviewFollowupPlaywrightTest#scopeSourcesAndNewCopyStayBilingualAndUserAuthoredTextStaysVerbatim,UiCleanupDefectRegressionPlaywrightTest#internalTransferReviewUsesRecipientAndTransferGuidanceNotSchoolOrFx' test
```

**9 test: 8 PASS, 1 FAIL.** Ca scope VI/EN đạt. Ca chuyển nội bộ mở dialog VI nhưng test kỳ vọng EN; bổ sung lựa chọn ngôn ngữ rõ ràng.

```powershell
mvn '-Dtest=BusinessUxPlaywrightTest,UiCleanupDefectRegressionPlaywrightTest#internalTransferReviewUsesRecipientAndTransferGuidanceNotSchoolOrFx' test
```

**8/8 PASS, BUILD SUCCESS.** Bao gồm popup tuition, chuyển nội bộ, mobile, hủy/Escape, báo giá hết hạn, Emergency Stop, chọn kênh, số liệu và thanh toán lặp.

```powershell
mvn '-Dtest=BusinessUxPlaywrightTest#finalReviewHasLockedFieldsAndCancelEscapeDoNotApprove+expiryWhileDialogIsOpenDisablesConfirmationWithoutSubmitting+mobileInternalTransferUsesCorrectLanguageAndFitsShortViewport' test
```

**3/3 PASS, BUILD SUCCESS.** Lượt cuối kiểm tra riêng footer cảnh báo báo giá và nút xác nhận trên các viewport, gồm 360×430; số tiền và trạng thái báo giá vẫn nhìn thấy, chi tiết dài cuộn riêng. Log: `business-ux-footer.log`.

### 4.3 Tổng các ca khác nhau đã đạt sau sửa tương ứng

| Lớp | Ca đã chạy/đạt trong các lượt trên |
|---|---:|
| BusinessUxPlaywrightTest | 7 |
| FinBridgePlaywrightE2ETest | 2 |
| PaymentWorkflowPlaywrightE2ETest | 5 |
| StudentExpensePlaywrightE2ETest | 3 |
| UiCleanupPlaywrightTest | 5 |
| UiProductRefinementPlaywrightTest | 6 |
| UiReviewFollowupPlaywrightTest | 4 |
| UnifiedAccountsPlaywrightTest | 3 |
| GlobalAssistantPlaywrightTest | 2 |
| UiCleanupDefectRegressionPlaywrightTest | 3 |
| **Tổng ca khác nhau** | **40** |

Đây là tổng các ca khác nhau đạt **qua nhiều lượt**, không phải một lần chạy 40/40 trên bản cuối. Những failure lịch sử được ghi ở trên. Không chạy lại full suite không liên quan. `DeployedAcceptanceIT` chỉ được cập nhật thao tác dialog và compile; **không chạy trên Render**.

## 5. Bằng chứng nghiệp vụ không bị thay đổi

- Chưa tích checkbox/xác nhận, hủy hoặc Escape: không POST approval, không receipt, không đổi snapshot dữ liệu tài chính.
- Hai phiên xem cùng plan, phiên thứ hai gửi lại approval sau phiên thứ nhất: cùng transaction ID, **một** Sandbox transaction, Bank A còn 29.239.200 VND. Đây là kiểm tra stale review/idempotent retry, không phải cuộc đua hai request đồng thời.
- Receipt kiểm tra debit 70.760.800 VND và credit 20.000 CNY; các test hồi quy liên quan đối chiếu phí, sổ giao dịch và số dư.
- Backend chặn recipient mismatch và expired quote ở test FinBridge; UI hết hạn còn được kiểm tra bằng clock của browser. Hai loại bằng chứng này khác nhau.
- Emergency Stop trong popup làm policy PAUSED, không gửi approval và không thực thi.
- Hai ca GlobalAssistant kiểm tra số dư hiện tại khác receipt lịch sử, và draft B thực thi đúng B trong khi workspace vẫn chọn A. AI ở hai ca này dùng mock.
- Thao tác highlight kênh/di chuyển carousel/đổi ngôn ngữ không làm thay đổi snapshot tài chính.

## 6. File thay đổi

- Production presentation: `src/main/resources/templates/home.html`, `src/main/resources/static/app.css`, `src/main/resources/static/app.js`.
- Test mới: `BusinessUxPlaywrightTest.java`, `PaymentApprovalControls.java`.
- Các lớp test browser trong bảng mục 4 được cập nhật expectation hoặc thao tác popup phù hợp; thêm `DeployedAcceptanceIT.java` để harness cloud tương thích nhưng chưa thực thi.
- Báo cáo này, ảnh và log tại `docs/business-ux/`.
- Các báo cáo/ảnh review ngoài task, `.agents/` và `ScreenshotEvidenceCollectorTest.java` có sẵn dạng untracked không được đưa vào commit này. Ảnh lịch sử của các test cũ được khôi phục, không ghi đè bằng chứng nghiệm thu trước.

## 7. Giới hạn và nghiệm thu còn lại

- Chưa kiểm tra điện thoại thật, bàn phím hệ điều hành, screen reader hoặc audit WCAG đầy đủ.
- Chưa kiểm tra UI mới trên Render; push không chứng minh phiên bản đã Live.
- Không thêm đăng nhập, tách user, ngân hàng thật, OTP/PIN, PDF biên nhận chính thức, locale framework mới hoặc AI phase mới.
- Tên trường/ngân hàng synthetic và nguồn quote mô phỏng được giữ nguyên; không có xác minh trực tiếp với trường hay ngân hàng.
- Receipt/Audit Log vẫn được giữ theo nghiệp vụ hiện có; người dùng không có quyền edit các thông tin đã thực thi.

### Checklist ngắn sau deploy

1. Xác nhận đúng SHA Live, tải lại bỏ cache.
2. Tổng quan: chỉ một khối số dư/kế hoạch; VI/EN rõ ràng.
3. Tạo plan ở local hoặc trong cửa sổ test đã phối hợp. Mở popup; đối chiếu bill/tài khoản/kênh/số tiền; hủy để xác nhận chưa thanh toán.
4. Điện thoại thật: cuộn phần chi tiết trong popup, kiểm tra nút xác nhận/cancel/dừng còn thấy; mở chat và bật bàn phím.
5. Nếu cần thanh toán/reset trên Render dùng chung, phối hợp team trước. Task này không thực hiện các thao tác đó.

**Kết luận:** phần UI được kiểm chứng bằng Chrome automation local và các ca hồi quy liên quan. Không suy diễn thành nghiệm thu cloud, model thật, thiết bị thật hoặc production cho tiền thật.
