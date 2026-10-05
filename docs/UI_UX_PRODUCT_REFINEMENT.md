# Hoàn thiện UI/UX FinBridge

## Phạm vi và checklist thực hiện

Baseline: 5065543. Stack hiện có: Spring Boot, Thymeleaf, JavaScript/CSS.
Chỉ dữ liệu synthetic và Payment Sandbox; không gọi Gemini, push hoặc deploy.

- [x] Quan sát và chụp giao diện trước sửa bằng Chrome, database test riêng.
- [x] Điều hướng: Tổng quan, Giao dịch, Tài chính du học, Thanh toán & Lịch sử; hướng dẫn lần đầu có thể đóng.
- [x] Tổng quan/Giao dịch: nguồn số dư, kỳ báo cáo, chi tiêu và ngân sách tách biệt; review và Undo rõ ràng.
- [x] Học phí: các bước chọn hóa đơn → xác minh → so sánh → review → duyệt → biên nhận.
- [x] Thanh toán: nội dung đúng trạng thái; số dư hiện tại/dự kiến/lịch sử phân biệt; quote/Bank B/Emergency Stop giữ nguyên.
- [x] Trợ lý: khả năng và đối tượng rõ; câu trả lời ưu tiên kết luận; giữ form, trạng thái chờ và lựa chọn theo phiên.
- [x] Ngôn ngữ: VI/EN theo nhãn/template đầy đủ; không dịch bằng thay từ; giữ nguyên dữ liệu người dùng.
- [x] Accessibility/responsive: focus, dialog, thông báo, reduced motion; 360/390/768/1440px.
- [x] Kiểm chứng liên quan, full suite cuối, ảnh trước/sau, so sánh các file được bảo vệ.

## Các file trình bày hiện có

- src/main/resources/templates/home.html: cả bốn màn hình, form, review và trợ lý.
- src/main/resources/templates/fragments/conversation.html: thẻ câu trả lời.
- src/main/resources/static/app.js: điều hướng, cập nhật tại chỗ, bản dịch, quote timer, chat.
- src/main/resources/static/app.css: layout và responsive.
- ConversationPresentation.java: formatter dữ liệu trả lời hiện có; không đổi phép tính.
- Các lớp *PlaywrightTest hiện có: nghiệm thu bằng Chrome và model mock.

## Giới hạn nền tảng

Tài khoản planner/Tổng quan và tài khoản thanh toán Sandbox chưa ánh xạ.
Giao diện phải nêu nguồn riêng, không cộng hai nguồn và không gọi ngân sách còn lại là số dư.
Workspace tài chính dùng chung; context chat theo phiên. Đây chưa phải hệ thống production tiền thật.


## Thay đổi người dùng nhìn thấy

- Thương hiệu FinBridge và bốn màn hình rõ ràng. Thanh toán & Lịch sử có trạng thái trống khi chưa có kế hoạch.
- Hướng dẫn đầu tiên ở Tổng quan, có thể đóng và nhớ lựa chọn; không chặn thao tác.
- Số dư tài khoản thanh toán tách riêng nguồn planner. Chi tiêu tháng có kỳ báo cáo; ngân sách còn lại không được gọi là tiền khả dụng.
- Lịch sử có filter, phân trang, nguồn tiền, loại giao dịch và trạng thái xem xét; mobile dùng hàng thẻ. Review, danh mục và Undo giữ nguyên.
- Học phí có sáu bước theo trạng thái backend. Kênh nêu tổng chi phí, số dư dự kiến, tiền nhận, phí, phụ phí tỷ giá, thời gian nhận và hiệu lực báo giá. Làm mới vẫn cần bấm nút.
- Bank B chỉ tham khảo, không có form tạo/thực thi thanh toán; chi tiết mở rộng giữ nguồn, phí và timestamp.
- Review phân biệt chờ duyệt, bị chặn, vô hiệu, hủy và hoàn tất. Biên nhận hoàn tất ở trên; thông tin kế hoạch/báo giá đã dùng có thể mở rộng.
- Nhãn “Số dư hiện tại”, “Số dư dự kiến sau thanh toán”, “Số dư ngay sau thanh toán này” phân biệt dữ liệu hiện tại, ước tính và lịch sử.
- Chat có nút mở toàn cục, chuyển màn hình rõ ràng, chi tiết kế hoạch thu gọn. Giữ draft, vị trí cuộn, shortcut review/receipt, form sinh hoạt và kiểm soát request.
- Bản dịch dùng nhãn hoặc mẫu câu hoàn chỉnh; đã bỏ global word replacement. Giữ nguyên tin nhắn và tên hóa đơn người dùng.
- Công cụ môi trường/reset/policy tách riêng; cảnh báo reset nói rõ ảnh hưởng cả team. Dừng khẩn cấp luôn dễ tìm.
- Focus nhìn thấy, dialog đóng bằng Escape/vùng ngoài và trả focus, lỗi nhập hiển thị tại trường. Motion tắt theo tùy chọn hệ thống.

## Kiểm chứng theo nhóm

Lệnh chạy tại `D:\personal-finance-cross-border-agent` bằng Maven 3.9.16, JDK 21.
Chrome thật headless qua Playwright hiện có; database H2 test riêng. Provider mock hoặc tắt.
**Số request Gemini/Ollama thật: 0.** Không sử dụng app local 8124 hay Render.

| Nhóm | Lệnh (sau `mvn`) | Kết quả đã quan sát |
|---|---|---|
| Ảnh baseline | `-Dtest=UiProductRefinementPlaywrightTest -Dui.refinement.stage=before test` | 1/1 PASS (lúc lớp mới chỉ có test capture) |
| Điều hướng | `-Dtest=UiProductRefinementPlaywrightTest#navigationIntroductionAndEnvironmentToolsAreDiscoverable test` | Gate cuối 1/1 PASS; lần đầu phát hiện overflow tablet và điều hướng màn hình trống, đã sửa |
| Tài chính | `-Dtest=UiProductRefinementPlaywrightTest#balancesBudgetsAndTransactionAmountsKeepSourcesAndCurrenciesDistinct,FinanceOverviewPlaywrightE2ETest test` | 2/2 PASS, gồm số lẻ và tiền tệ riêng |
| Học phí/thanh toán | `-Dtest=PaymentWorkflowPlaywrightE2ETest,StudentExpensePlaywrightE2ETest,PaymentSourcePlaywrightE2ETest test` | 13/14 đạt lần đầu; một assertion tiêu đề báo giá cũ. Đã cập nhật nhãn và test đó đạt ở vòng sau |
| Assistant | `-Dtest=AssistantPanelPlaywrightTest,AssistantAnswerPresentationPlaywrightTest,GlobalAssistantPlaywrightTest,StudentExpensePlaywrightE2ETest#expenseAndCompactChannelFlowWorksInBrowser test` | Lần đầu phát hiện form bị che khi bàn phím mở; đã chỉnh layout. Các lỗi khác là expectation nhãn/vị trí Reset cũ |
| Gate cuối | `-Dtest=UiProductRefinementPlaywrightTest,AssistantAnswerPresentationPlaywrightTest#runwayChunksStayOneAnswerAndFormAndComposerFitReducedVisualViewport,HostingDemoPlaywrightTest#hostingUsesEphemeralDatabaseAndShowsBilingualSharedNotice,ContextualConversationPlaywrightTest#targetedChoicesAndSessionIsolationWorkThroughTheSharedPanel test` | 9/9 PASS |
| Sửa/xóa giả định | `-Dtest=RunwayPlaywrightTest#mobileStructuredInputConfirmationEditClearAndResetAreReadonly test` | 1/1 PASS; mở “Giải thích về ước tính” trước khi Xóa |

Các lượt lỗi và gate trung gian ở `target/ui-ux-*.log`; kết quả cuối được tổng hợp bên dưới.
Báo cáo và ảnh trong docs tồn tại sau `mvn clean`.

## Ảnh và responsive

- Trước: `docs/ui-ux/before/`, 12 ảnh ở 1440×1000 và 390×844.
- Sau: `docs/ui-ux/after/`, 28 ảnh: dashboard, transactions, student, review, assistant, receipt và receipt-vi.
- Viewport sau: 1440×1000, 768×1000, 390×844, 360×1000.
- Test đo `document.documentElement.scrollWidth <= innerWidth + 1` trên tài chính, học phí, review, biên nhận và chat.
- Kiểm tra hình học bàn phím bằng visual viewport 390×430 và 390×337; input mức chi, nút xác nhận, composer và nút Gửi nằm trong vùng nhìn thấy.
- Kiểm tra trực quan ảnh desktop/mobile, phát hiện hướng dẫn Tổng quan xuất hiện sai trên URL review; đã sửa và thêm assertion.
- Đây là browser emulation, **chưa xác minh trên điện thoại vật lý**, screen reader hay Safari.

## Bảo vệ hành vi tài chính

Checksum trước sửa cho toàn bộ Java main, cấu hình/schema resource, pom và requirements.
Đối chiếu SHA-256: **35/36 file giống baseline**. Chỉ `ConversationPresentation.java` khác: thêm `money(Number)` gọi formatter string hiện có, giữ mọi chữ số thập phân, không tính hay làm tròn.
Service tài chính, session/object resolution, intent/provider/schema, policy/execution/Sandbox/receipt giữ nguyên.
Không thêm dependency, endpoint, cấu hình, schema hoặc corridor.
Form thanh toán giữ endpoint, tham số khóa và điều kiện server; UI không tự duyệt, gửi lại hoặc làm mới báo giá.

## Giới hạn và phần chưa kiểm chứng

- Môi trường mô phỏng dùng chung, chưa có đăng nhập/tách người dùng hoặc tiền thật.
- Planner và nguồn thanh toán chưa ánh xạ; UI nêu giới hạn, không cộng trùng hay suy diễn.
- Báo giá và registry synthetic; tên riêng/ID/file người dùng không dịch.
- Mobile chỉ Chrome emulation. Không dùng Gemini thật cho phase trình bày.
- Không push/deploy; Render chưa có giao diện này. Cần restart local để đọc template/static mới.
- Không thay đổi artifact `.agents/`, `docs/UI_UX_TEAM_REVIEW.md`, `docs/ui-ux-team-review/` và `ScreenshotEvidenceCollectorTest.java` từ công việc khác trong workspace; không thuộc commit UI này.

## Full suite và Definition of Done


### Lệnh và kết quả cuối

Lệnh PowerShell thực tế, chạy trong thư mục repository:

```powershell
& 'C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd' test *> target/ui-ux-full-suite-final.log
```

- **318 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS.**
- Hoàn tất 05/10/2026 10:56:40 UTC+7, thời gian **4 phút 22 giây**.
- 40 lớp được thực thi; **70 kiểm tra Chrome/Playwright**, gồm 6 ca UI mới và 4 ca collector hiện có từ công việc khác. 314 test thuộc bộ hiện tại + UI phase này; collector 4 test được chạy nhưng file không thuộc commit.
- Lượt full suite trước: 318 tests, 2 failures, 0 errors. Một expectation nhãn tiếng Việt cũ, một selector desktop dùng trên mobile. Đã sửa selector/nhãn, giữ assertion hành vi. Kiểm tra trực quan cũng phát hiện hướng dẫn Tổng quan ở URL review; sửa và thêm assertion trước lượt full suite cuối.
- Gate tiếng Việt cuối: `mvn -Dtest=FinBridgePlaywrightE2ETest#languageSwitcherChangesVisibleCopyAndPersistsAcrossReload test`: **1/1 PASS**.
- `git diff --check`: PASS, không lỗi whitespace.
- **0 request Gemini/Ollama thật**. Không chạm vào database local 8124, Render hoặc dữ liệu dùng chung trên hosting.

### Bằng chứng cho 12 điều kiện nghiệm thu

Các lớp dưới đều đã chạy thành công trong full suite cuối; provider là mock hoặc bị tắt.

| Điều kiện | Kết quả | Bằng chứng |
|---|---|---|
| Người mới tìm số dư, giao dịch, học phí và trợ lý | PASS | UiProductRefinementPlaywrightTest.navigationIntroductionAndEnvironmentToolsAreDiscoverable; intro đóng và nhớ lựa chọn |
| Câu hỏi được hỗ trợ dùng từ mọi tab | PASS | GlobalAssistantPlaywrightTest.fixedCorpusHasSameResultFromEveryTabWithoutContextButtonsOrFinancialMutations |
| Lựa chọn trong chat không đổi lựa chọn tài chính chung | PASS | GlobalAssistantPlaywrightTest.pendingAccountChoiceIsVisibleAndBoundOnlyToThisSession; ContextualConversationPlaywrightTest |
| Chat chọn B, draft/review/thanh toán B; workspace giữ A | PASS | GlobalAssistantPlaywrightTest.draftBFromChatReviewsAndPaysBOnlyWhileAStaysSelected |
| Tạo draft chưa duyệt, không ledger/payment/receipt | PASS | GlobalAssistantPlaywrightTest.draftBFromChatReviewsAndPaysBOnlyWhileAStaysSelected; UiProductRefinementPlaywrightTest.stateSpecificReviewAndReceiptRemainAuthoritative |
| Duyệt riêng, thực thi đúng kế hoạch một lần | PASS | UiProductRefinementPlaywrightTest.stateSpecificReviewAndReceiptRemainAuthoritative; PaymentWorkflowPlaywrightE2ETest; PhaseFourIntegrationTest |
| Hoàn tất có receipt, không còn nhắc chờ duyệt | PASS | UiProductRefinementPlaywrightTest.stateSpecificReviewAndReceiptRemainAuthoritative; GlobalAssistantPlaywrightTest.everyPlanStateHasAnAccurateBilingualBanner |
| Số dư hiện tại/dự kiến/lịch sử khác nhau rõ | PASS | GlobalAssistantPlaywrightTest.historicalAndCurrentBalancesRemainDifferentAcrossAllTabsAfterActualPayment; UiProductRefinementPlaywrightTest; AssistantAnswerPresentationPlaywrightTest |
| Bank B/quote hết hạn/vô hiệu/Emergency Stop giữ an toàn | PASS | UiProductRefinementPlaywrightTest.expiredUnavailableInvalidatedAndEmergencyStatesKeepRecoveryVisible; PhaseFourIntegrationTest; PhaseFiveIntegrationTest; AssistantPanelPlaywrightTest |
| Reset nêu ảnh hưởng chung | PASS | UiProductRefinementPlaywrightTest.navigationIntroductionAndEnvironmentToolsAreDiscoverable; HostingDemoPlaywrightTest.hostingUsesEphemeralDatabaseAndShowsBilingualSharedNotice |
| Việt–Anh và dữ liệu người dùng | PASS | UiProductRefinementPlaywrightTest.bilingualLabelsUserTextDialogsAndMobileTargetsStayUsable; FinBridgePlaywrightE2ETest.languageSwitcherChangesVisibleCopyAndPersistsAcrossReload |
| Desktop/mobile/keyboard/dialog usable | PASS trong Chrome | UiProductRefinementPlaywrightTest; AssistantAnswerPresentationPlaywrightTest.runwayChunksStayOneAnswerAndFormAndComposerFitReducedVisualViewport; AssistantPanelPlaywrightTest |

### Danh sách file của task

**Trình bày (4):**

- `src/main/resources/templates/home.html`
- `src/main/resources/static/app.css`
- `src/main/resources/static/app.js`
- `src/main/java/com/example/finance/ConversationPresentation.java` — chỉ bổ sung formatter, không đổi service tài chính.

**Tests (9):**

- `src/test/java/com/example/finance/UiProductRefinementPlaywrightTest.java` — mới, 6 ca.
- `src/test/java/com/example/finance/AssistantPanelPlaywrightTest.java`
- `src/test/java/com/example/finance/ContextualConversationPlaywrightTest.java`
- `src/test/java/com/example/finance/FinBridgePlaywrightE2ETest.java`
- `src/test/java/com/example/finance/HostingDemoPlaywrightTest.java`
- `src/test/java/com/example/finance/PaymentWorkflowPlaywrightE2ETest.java`
- `src/test/java/com/example/finance/PersonalFinanceAiPlaywrightTest.java`
- `src/test/java/com/example/finance/RunwayPlaywrightTest.java`
- `src/test/java/com/example/finance/StudentExpensePlaywrightE2ETest.java`

**Bằng chứng (41):**

- `docs/UI_UX_PRODUCT_REFINEMENT.md`.
- 12 PNG trong `docs/ui-ux/before/`.
- 28 PNG trong `docs/ui-ux/after/`.

### Definition of Done: PASS

UI/UX trong phạm vi đã triển khai; luồng hiện có và kiểm tra trình duyệt/full suite đạt; ảnh/báo cáo được lưu trong docs; file nghiệp vụ được bảo vệ không đổi. Chỉ formatter trình bày được bổ sung theo phạm vi cho phép.

Đây là kết luận cho phase trình bày trên local, không phải nghiệm thu production tiền thật hoặc nghiệm thu phiên bản Render. Không còn blocker cho commit UI/UX. Commit chỉ các file task, không push/deploy hoặc bắt đầu AI phase khác.
