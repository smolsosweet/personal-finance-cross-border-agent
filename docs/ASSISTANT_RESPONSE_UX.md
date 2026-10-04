# Kiểm chứng UX câu trả lời FinBridge — 04/10/2026

## Phạm vi đã hoàn thành

- Kết luận được đưa lên đầu cho chi tiêu, ngân sách, dự báo sau học phí và số tháng sinh hoạt.
- Bảng ngân sách thể hiện hạn mức, đã chi, còn lại và vượt; có cuộn ngang khi cần.
- Chi tiêu, tiền tệ và hoàn tiền được hiển thị riêng. Định dạng số giữ nguyên các chữ số, không tính lại hoặc làm tròn tiền ở giao diện.
- So sánh dùng thẻ, ghi đúng số kênh được trả về (tối đa 3) và tiêu chí: tổng chi phí thấp nhất hoặc thời gian tối đa nhanh nhất, rồi tổng chi phí.
- Nguồn, quote ID, công thức và giả định nằm trong **Xem chi tiết / View details**. Toàn bộ câu trả lời gốc vẫn được giữ trong phần này.
- **ƯỚC TÍNH / ESTIMATE**, giới hạn chưa ánh xạ tài khoản, thiếu tiền, yêu cầu phê duyệt và trạng thái quote vẫn hiện ngoài phần thu gọn.
- Quote trong chat có đếm ngược và chuyển sang cảnh báo hết hạn mà không cần F5. Đây là hiển thị thời gian; backend vẫn kiểm tra tính hợp lệ khi tạo/duyệt plan.
- Panel thích ứng visual viewport. Khi bàn phím làm vùng hiển thị nhỏ, form có thể co lại và cuộn riêng; ô chat và nút xác nhận không bị che trong các cấu hình đã kiểm chứng.
- Giữ ngôn ngữ của từng lượt trả lời. Không dịch từng từ bên trong dữ liệu của một câu trả lời cũ khi chuyển ngôn ngữ giao diện.
- Các chunk của cùng một lượt được gộp để hiển thị, bằng ID hiển thị phía server. Hai câu trả lời độc lập không bị gộp; lịch sử hiển thị vẫn không phải ngữ cảnh đáng tin cho model.

Không thêm intent, tính năng AI, quyền thanh toán, tích hợp bên ngoài hay deployment.

## File thay đổi

| File | Thay đổi |
| --- | --- |
| `src/main/java/com/example/finance/ConversationPresentation.java` | Adapter trình bày template backend; không tính toán, suy diễn intent hoặc thực thi |
| `src/main/java/com/example/finance/SessionConversationService.java` | ID gộp chunk cho hiển thị; thêm nhãn số kênh và tiêu chí vào câu so sánh |
| `src/main/resources/templates/fragments/conversation.html` | Fragment câu trả lời dùng chung cho tổng quan và panel |
| `src/main/resources/templates/home.html` | Dùng fragment và cập nhật phiên bản asset |
| `src/main/resources/static/app.css` | Kết luận, bảng/thẻ, cảnh báo, chi tiết và panel nhỏ |
| `src/main/resources/static/app.js` | Giữ ngôn ngữ câu trả lời, đếm ngược quote, cuộn và visual viewport |
| `src/test/java/com/example/finance/AssistantAnswerPresentationPlaywrightTest.java` | 6 browser tests mới, classifier được mock |
| `docs/ASSISTANT_RESPONSE_UX.md` | Báo cáo này |

Các file sau được đối chiếu bằng `git diff --name-only` và **không thay đổi**: `PhaseFourService.java`, `CrossBorderService.java`, `PersonalFinanceInsights.java`, `LivingExpenseRunwayService.java`, `FinanceChatService.java`, `LlmIntentContract.java`, các provider/client LLM và `schema.sql`. Không đổi phép tính, Policy Guard, Payment Sandbox hoặc schema/contract AI.

## Môi trường và lệnh đã chạy

Windows; JDK 21.0.6, Maven 3.9.16, Playwright 1.63.0, Chrome hiện có, Ollama tại `localhost:11434`, model `qwen3:4b` được xác nhận qua `/api/tags`.

Test dùng H2 memory và dữ liệu seed tổng hợp, tách với ứng dụng đang chạy tại cổng 8080. Phần thiết lập Java cho các lệnh Maven:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

### Bộ liên quan

```powershell
mvn '-Dtest=AssistantAnswerPresentationPlaywrightTest,AssistantPanelPlaywrightTest,RunwayPlaywrightTest,PersonalFinanceAiPlaywrightTest,ContextualConversationPlaywrightTest,SessionConversationIntegrationTest' test
```

**40/40 đạt**, 0 failure/error/skip; 1 phút 9 giây. Lúc chạy, test mới có 5 trường hợp. Sau đó thêm trường hợp thứ 6 kiểm tra nhiều tiền tệ, hoàn tiền và phần thập phân; trường hợp này đã chạy đạt trong các lệnh sau.

### Chrome với model thật và test UX bổ sung

```powershell
mvn '-Dtest=AssistantAnswerPresentationPlaywrightTest,RunwayOllamaLiveIT' test
```

**8/8 đạt**, 0 failure/error/skip; 2 phút 2 giây: 6 test UX dùng mock classifier, 2 test qua Chrome với Qwen thật, Anh và Việt. Hai test thật thực hiện tổng cộng 10 lần phân loại hợp lệ; không tính fallback là model pass. Thời gian lượt đầu 36,583 giây; các lượt sau 5,416–6,271 giây.

Các test thật xác nhận 8 triệu/tháng → 3,27 tháng; 10 triệu → 2,62 tháng; 9 triệu → 2,91 tháng. Xác nhận form không gọi LLM. Hỏi CNY không tự chuyển đổi tiền tệ. Snapshot tài chính không đổi; plan, payment, ledger, receipt đều không được tạo. Reset xóa mức chi đã xác nhận.

### Sửa và kiểm chứng bàn phím màn hình nhỏ

Thử thêm viewport cao 337 px đã phát hiện ô chat bị đẩy khỏi panel. Đã sửa co dãn form, giữ cỡ chữ input mobile 16 px và thu gọn dòng mức chi trùng với giá trị input khi vùng hiển thị rất nhỏ.

Lệnh đã chạy thực tế sau sửa:

```powershell
mvn '-Dtest=AssistantAnswerPresentationPlaywrightTest,AssistantPanelPlaywrightTest#mobileLayoutKeyboardAndEnglishContextFitWithoutDemoTools,RunwayPlaywrightTest#monthlyFormStaysWithinReachAfterLongHistoryOnDesktopAndMobile' test
```

**7/7 đạt**: 6 test mới và 1 test lịch sử dài. Filter method của `AssistantPanelPlaywrightTest` trong lệnh này bị gõ sai, nên không chạy test đó. Đã chạy riêng đúng tên sau đó:

```powershell
mvn '-Dtest=AssistantPanelPlaywrightTest#mobilePanelFitsLanguageAndKeyboardWorkWithoutExposingDemoControls' test
```

**1/1 đạt**, 12,539 giây. Test viewport mới kiểm tra cả 430 px và 337 px: input sinh hoạt, nút xác nhận, input chat và nút gửi đều nằm trong vùng hiển thị và không bị phần tử khác che. Đây là mô phỏng hình học bàn phím, không phải thử bàn phím trên thiết bị thật.

Tổng cộng **43 test khác nhau đã chạy đạt trong các checkpoint liên quan**: 41 dùng mock classifier hoặc kiểm tra backend và 2 dùng Qwen thật. Không chạy lại toàn bộ suite không liên quan. Những số trên không cộng lặp lại các test đã rerun.

### Các lượt chưa đạt ban đầu

- Lệnh `mvn '-Dtest=AssistantPanelPlaywrightTest,RunwayPlaywrightTest,PersonalFinanceAiPlaywrightTest' test`: 13 test, 3 timeout ở tải trang/thao tác. Log có khoảng gián đoạn thời gian dài; chưa xác định chắc chắn nguyên nhân. Cả ba trường hợp đạt trong lần chạy bộ liên quan tiếp theo; không nới timeout/guard để ép pass.
- Lệnh `mvn '-Dtest=AssistantAnswerPresentationPlaywrightTest' test` ban đầu: 5 test, 2 failure do setup mô phỏng viewport phát sai sự kiện và câu hỏi chưa có chủ đề được backend yêu cầu làm rõ. Đã dùng sự kiện resize đang đăng ký và câu hỏi đầy đủ trong fixture; không đổi logic làm rõ của sản phẩm.
- Lệnh `mvn '-Dtest=AssistantAnswerPresentationPlaywrightTest#runwayChunksStayOneAnswerAndFormAndComposerFitReducedVisualViewport' test` khi thêm 337 px: 1 failure, lỗi UX thực về ô chat nêu trên. Đã sửa và test đạt sau đó.

Log/ảnh trong `target/answer-ux-*` là artifact local, không commit vào Git. Đã xem ảnh ngân sách, so sánh, runway mobile và cả hai hình học bàn phím.

## Kiểm tra tương tác trực tiếp và hạn chế

Đã khởi động ứng dụng riêng trên cổng 8118:

```powershell
mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=8118 --spring.profiles.active=local --spring.datasource.url=jdbc:h2:mem:answer_ux_manual;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1 --app.demo-tools-enabled=false --finbridge.llm.enabled=true --finbridge.llm.provider=ollama --finbridge.llm.model=qwen3:4b --finbridge.llm.base-url=http://localhost:11434'
```

Log xác nhận Tomcat chạy và database `jdbc:h2:mem:answer_ux_manual`. Công cụ browser tương tác không mở được, lỗi **`trusted Node process exited unexpectedly; kernel reset, rerun your request`** ở cả hai lần thử. Không ghi nhận luồng kiểm tra tương tác đó là pass. Các browser tests qua Chrome/Playwright nêu trên đã thực sự chạy. Ứng dụng kiểm chứng 8118 đã được dừng; giữ ứng dụng 8080 và Ollama của người dùng.

Hạn chế còn lại:

- Chưa kiểm tra bàn phím thật trên Android/iOS. Cần bước này trước demo trên điện thoại/deploy cho team.
- Cold model vẫn có thể chậm; lượt đầu lần này 36,583 giây. Giữ trạng thái đang xử lý, timeout/fallback và chống gửi trùng hiện có.
- Adapter phụ thuộc template backend hiện tại. Mẫu không nhận diện được hiển thị nguyên văn; khi thêm intent/template cần bổ sung presentation/test tương ứng.
- Chưa xác định chắc chắn nguyên nhân 3 timeout ban đầu; kết quả chạy lại liên quan đạt và được ghi tách biệt.

## Checklist kiểm tra trên máy của bạn

1. Dừng app cũ bằng `Ctrl+C`, rồi chạy `.\scripts\start-local.ps1` để nạp bean/template mới. Dữ liệu local vẫn theo cấu hình hiện có.
2. Hỏi ngân sách bằng Việt và Anh: kết luận/bảng ở đầu, nguồn ở phần thu gọn; mở **Xem chi tiết** để đối chiếu. Ngân sách còn lại không được trình bày như số dư tài khoản.
3. Với seed Bank A 100 triệu VND, học phí 20.000 CNY và quote còn hiệu lực: hỏi số tháng sinh hoạt, xác nhận 8 triệu/tháng → kết luận 3,27 tháng kèm **ƯỚC TÍNH**. Đổi 10 triệu → 2,62; 9 triệu → 2,91. Không có plan/receipt chỉ vì hỏi.
4. Hỏi kênh rẻ nhất rồi nhanh nhất: thấy số kênh thực tế và tiêu chí đúng; Bank B không xuất hiện trong các thẻ đủ điều kiện. Chờ quote hết hạn để kiểm tra cảnh báo không cần F5.
5. Trên điện thoại thật, chạm input sinh hoạt và input chat; mở/đóng bàn phím, xoay màn hình. Kiểm tra input/nút xác nhận/nút gửi còn dùng được; form cuộn riêng trong vùng rất nhỏ. Đóng bàn phím để xem lại toàn bộ kết quả.

Checkpoint này chỉ hoàn thiện UX. Chưa deploy, chưa bắt đầu phase AI tiếp theo hoặc production hardening.
