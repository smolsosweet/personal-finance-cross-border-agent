# Chat FinBridge trong không gian làm việc

Ngày kiểm tra: 02/10/2026. Phạm vi: giúp người dùng tìm và sử dụng chat hiện có từ mọi tab; không bổ sung intent AI hay thay đổi thực thi tài chính.

## Cách sử dụng

1. Tải lại trang. Nút **Hỏi FinBridge / Ask FinBridge** luôn ở góc dưới bên phải khi panel đóng.
2. Bấm nút để mở panel bên cạnh trên desktop; trên điện thoại panel gần toàn màn hình. Dùng **×** hoặc **Escape** để đóng, bản nháp vẫn được giữ trong lần mở trang hiện tại.
3. Trong Tổng quan/Giao dịch, có gợi ý chi tiêu và ngân sách. Trong Tài chính du học, có gợi ý khả năng đóng học phí, so sánh kênh và Bank B không khả dụng. Gợi ý chỉ điền câu hỏi; bấm **Gửi** mới gửi yêu cầu.
4. **Hỏi về học phí** trong tab du học mở panel và điền câu hỏi được hỗ trợ. **Giải thích kế hoạch này** tại thanh toán mở bảng thông tin của đúng kế hoạch đang xem, gồm tài khoản, người nhận, kênh, phí, tổng tiền và số dư còn lại.
5. Nếu chat tạo một kế hoạch, bấm **Xem kế hoạch thanh toán** để đến màn hình xem xét. Người dùng vẫn phải bấm nút phê duyệt riêng. Panel không có nút thực thi thanh toán.
6. Có thể đổi VI/EN ngay trong panel. **Dừng khẩn cấp** cũng nằm trong panel để sử dụng khi màn hình điện thoại che header.

Chat lớn tại Tổng quan được giữ lại và dùng chung lịch sử với panel. Công cụ demo/chính sách vẫn nằm riêng trong phần thu gọn; cấu hình hiện có `APP_DEMO_TOOLS_ENABLED=false` ẩn các công cụ này. Không thêm endpoint hoặc quyền mới.

## Ranh giới và xử lý lỗi

- Tất cả composer sử dụng endpoint hiện có `POST /agent/message` và cùng khóa gửi trùng/trạng thái chờ. Không sửa schema intent, confidence, prompt-injection guard hay Policy Guard.
- Thông tin kế hoạch trong panel được Thymeleaf render từ cùng `latestAction`/`paymentReview` với màn hình duyệt. Đây là giải thích bằng dữ liệu backend, không phải model tự tính tiền hoặc tự đọc toàn bộ ngữ cảnh màn hình.
- Gợi ý AI chỉ thuộc các intent đã hỗ trợ. Câu hỏi tài chính vẫn dùng dữ liệu đã ghi nhận và các giả định/ước tính đã có; không khẳng định mọi hóa đơn hoặc mọi câu hỏi đều được hỗ trợ.
- Mở/đóng panel, đổi tab, đổi ngôn ngữ hoặc đọc thông tin kế hoạch không gọi model và không tạo/phê duyệt thanh toán.
- Khi mất kết nối/timeout: hiện lỗi trong panel, giữ câu hỏi, khôi phục nút gửi; không tự retry. Sau Dừng khẩn cấp/reset, phản hồi frontend cũ không ghi đè UI mới.
- Backend hiện có có thể lưu một kế hoạch `BLOCKED` khi model trả kết quả tạo kế hoạch sau Dừng khẩn cấp. Policy Guard trả `AGENT PAUSED`, không có receipt hoặc thực thi. Reset loại bỏ kết quả yêu cầu thuộc phiên dữ liệu cũ.

## Kiểm tra đã thực thi

Môi trường: Windows, Java 21.0.6, Maven 3.9.16, Chrome qua Playwright có sẵn trong repository. Chỉ dùng H2 in-memory và dữ liệu tổng hợp khi thực hiện hành động; không đổi dữ liệu của ứng dụng người dùng ở 8080.

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn "-Dtest=AssistantPanelPlaywrightTest,PersonalFinanceAiPlaywrightTest,PaymentWorkflowPlaywrightE2ETest" test
mvn "-Dtest=AssistantPanelPlaywrightTest,AssistantPanelOllamaLiveIT" test
node --check src/main/resources/static/app.js
git diff --check
```

| Lệnh/kiểm tra | Kết quả quan sát |
| --- | --- |
| Bộ 3 lớp browser liên quan, lượt đầu | 15 test: 1 failure và 1 error. Selector VI trùng sau bổ sung UI; kỳ vọng test mới về không có plan sau Stop không khớp hành vi lưu plan bị chặn của backend. Đã sửa selector UI và kỳ vọng an toàn đúng với implementation hiện có, không sửa Policy Guard. |
| Bộ 3 lớp browser liên quan, chạy lại | **15/15 PASS**, 0 failure/error/skipped, BUILD SUCCESS, 49.091 giây, kết thúc 20:49:12 +07. Gồm 6 test panel, 4 test chat cũ và 5 test workflow thanh toán. |
| Panel mới và browser với Ollama thật | **7/7 PASS**, 0 failure/error/skipped, BUILD SUCCESS, 58.949 giây, kết thúc 20:52:01 +07. Chạy lại 6 test panel sau chỉnh lời chào và thêm 1 test live chứa 2 lời gọi model. |
| GET `http://localhost:11434/api/tags` | Truy cập được, `qwen3:4b` đã cài. |
| Qwen thật: “Ngân sách tháng này của tôi còn bao nhiêu?” | Intent `EXPLAIN_BUDGET_STATUS`; **24,982 ms** (24.982 giây) từ Send đến UI cập nhật; có nguồn ngân sách trong câu trả lời tiếng Việt; snapshot dữ liệu tài chính không đổi. |
| Qwen thật: “Prepare the cheapest tuition payment draft.” | Intent `CREATE_TUITION_PLAN`; **4,985 ms** (4.985 giây); plan `AWAITING_APPROVAL`, permission `APPROVAL`, **0 payment, 0 receipt**. Panel dẫn đến nút duyệt riêng và tổng tiền trùng màn hình duyệt. |
| GET ứng dụng người dùng `http://localhost:8080` | HTTP 200, HTML chứa launcher mới và phiên bản assets `20261002-assistant`. Chỉ đọc trang. |
| `node --check` và `git diff --check` | PASS. |

**Phân biệt:** `AssistantPanelPlaywrightTest` và `PersonalFinanceAiPlaywrightTest` chạy Chrome/application thật nhưng mock classifier. `PaymentWorkflowPlaywrightE2ETest` kiểm tra guided workflow hiện có. `AssistantPanelOllamaLiveIT` không mock provider/classifier; thực sự gọi Ollama. Không chạy toàn bộ suite không liên quan. Có **16 trường hợp test khác nhau** trong phạm vi này.

Các trường hợp panel bao gồm: mở từ nhiều tab; giữ draft; EN/VI; gợi ý không tự gửi; đồng bộ lịch sử với Tổng quan; trạng thái chờ và chống gửi trùng; lỗi mạng/timeout và retry thủ công; approval riêng; số tiền đúng kế hoạch; mobile 390×844; Escape/focus; ẩn công cụ demo; Dừng khẩn cấp và reset khi model đang chờ.

Đã xem ảnh do browser tạo để kiểm tra bố cục desktop/mobile:

- `target/assistant-student-desktop-vi.png`
- `target/assistant-plan-desktop-en.png`
- `target/assistant-mobile-vi.png`
- `target/assistant-ollama-live.png`

Đây là kiểm tra browser tự động và xem ảnh kết quả; không tuyên bố có một lượt thao tác thủ công độc lập qua ứng dụng 8080. Test instance ở 8101/8102 dừng khi test kết thúc. Ảnh và báo cáo Surefire nằm trong `target`, không commit.

## Files thay đổi

- `src/main/resources/templates/home.html`: launcher, panel, lối vào theo màn hình, thông tin kế hoạch và assets version.
- `src/main/resources/static/app.js`: mở/đóng, focus, EN/VI, gợi ý, đồng bộ lịch sử/context, phục hồi composer.
- `src/main/resources/static/app.css`: responsive layout, nút và chữ trong panel.
- `src/test/java/com/example/finance/AssistantPanelPlaywrightTest.java`: 6 trường hợp browser có mock classifier.
- `src/test/java/com/example/finance/AssistantPanelOllamaLiveIT.java`: test live opt-in, 2 lời gọi Qwen thật.
- `README.md` và tài liệu này: hướng dẫn sử dụng, phạm vi và bằng chứng kiểm tra.

`src/main/java`, `schema.sql`, application configuration, dependencies và code tài chính được bảo vệ không thay đổi.

## Giới hạn còn lại

- Lượt model đầu đo được gần 25 giây; UI hiển thị chờ, giữ timeout và fallback hiện có. Chưa tối ưu model hoặc bắt đầu phase AI mới.
- Context màn hình dùng để trình bày dữ liệu và gợi ý; không có cơ chế LLM hiểu tự do mọi kế hoạch hoặc lịch sử hội thoại mới.
- Draft/open state chỉ được giữ trong lần mở trang hiện tại; reload không phục hồi draft. Lịch sử hội thoại lấy từ backend.
- Kiểm tra responsive sử dụng Chrome desktop và viewport điện thoại giả lập; không xác nhận Safari/iOS hoặc bàn phím thật trên điện thoại.
- Không có lỗi còn mở trong các trường hợp vừa chạy. Kết quả này không thay thế audit đầy đủ hay kiểm thử những module không liên quan.

Thay đổi được commit cục bộ theo yêu cầu; không push và không bắt đầu phase tiếp theo.
