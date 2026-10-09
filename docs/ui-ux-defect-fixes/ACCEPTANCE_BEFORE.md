# Nghiệm thu UI/UX — VERIFY-ONLY

Ngày 05/10/2026. HEAD: 2d161f9e0608f83102c387738c86b72e20f358e7.
Local http://localhost:8158; DB jdbc:h2:mem:cleanup_acceptance_2d161f9 (MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1). LLM tắt. Không thao tác Render, gọi Gemini, sửa production/config/schema/dependency, commit/push/deploy. Git diff tracked rỗng.

## 1. Test tự động đã chạy

Tận dụng bằng chứng 56 test cũ trong docs/UI_UX_CLEANUP.md; không chạy lại full suite.
Chạy bổ sung:

```powershell
& 'C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd' '-Dtest=PaymentWorkflowIntegrationTest#completedSandboxPaymentIsLinkedToImmutableTransactionHistoryAndIsIdempotent' test
```

1 test, 0 fail/error/skipped, BUILD SUCCESS, 6,696 giây. Log: ../cleanup-acceptance-idempotency.log. Kiểm tra history/receipt bất biến và retry không thêm giao dịch.

## 2. Walkthrough bằng browser automation

Công cụ tương tác in-app và Chrome đều BLOCKED: trusted Node process exited unexpectedly / node_repl kernel exited unexpectedly; diagnostics windows sandbox failed: helper_unknown_error: setup refresh had errors.
Fallback đã thực sự chạy Playwright Java 1.63 trên Chrome headless với jar có sẵn, không cài dependencies. Đây KHÔNG phải kiểm tra thủ công hoặc điện thoại thật.

```powershell
$walkClasspath = (Get-Content -LiteralPath target/cleanup-acceptance/classpath.txt -Raw).Trim()
java --class-path $walkClasspath target/cleanup-acceptance/BrowserWalkthrough.java
java --class-path $walkClasspath target/cleanup-acceptance/VisualProbe.java
```

Harness và bằng chứng chỉ ở target, Git bỏ qua. Lượt đầu thiếu jar opentest4j trong classpath; sau khi bổ sung đường dẫn jar có sẵn, đã chạy lại từ seed sạch. Probe đầu dùng sai overload chờ; đã sửa harness tạm và chạy lại. Lỗi harness không phải kết quả ứng dụng. Kết quả sau cùng:

| Ca | Trạng thái | Quan sát |
|---|---|---|
| Tổng quan/header/chat | PASS | Mode, ACTIVE, môi trường mô phỏng rõ; một composer toàn cục, không chat inline dư; seed 315.000.000 VND. |
| Phân trang | PASS | 5 giao dịch/trang, Page 1 of 5; Next/Previous đúng; không cuộn dọc lồng. |
| Chat dài/đóng mở | PASS | Giữ vị trí 682 px và bản nháp chưa gửi; hỏi chỉ đọc không tạo plan/đổi số dư. Chỉ kiểm tra fallback vì LLM tắt. |
| Review draft | PASS | ACT-37EFB1E5-01B, Bank A Everyday, đúng người nhận/kênh; 70.760.800 VND; AWAITING_APPROVAL, chưa receipt; hủy xác nhận không gửi approval. |
| Approval/receipt | PASS | SBOX-6F343E08-40F; debit 70.760.800, convert 70.400.000, fee 360.800 VND, credit 20.000 CNY; số dư 29.239.200 VND, Tổng quan khớp. |
| Retry/audit/shortcut | PASS | Retry cùng key giữ receipt và số dư; audit có thực thi/retry; View receipt tại đáy chat mở đúng biên nhận. |
| Emergency Stop | PASS | AGENT PAUSED, không duyệt/tạo receipt mới; lý do backend đúng Anh–Việt; resume không tự thanh toán. |
| Responsive cơ bản | PASS trong phạm vi đo | 1366x768,1024x768,768x1024,390x844,360x640: không overflow ngang; chat/input/send/approval tiếp cận được. 390x450 kiểm tra viewport ngắn, không thay thế bàn phím thật. |
| Chất lượng bố cục | FAIL | Hai lỗi layout và một hướng dẫn sai ngữ cảnh bên dưới. Assertion visibility không chứng minh toàn bộ UI đạt. |
| Browser lỗi/traffic | PASS | Không JS page error; không request browser ngoài localhost. |

walkthrough-results.json ghi 8 nhóm assertion đạt. visual-probe.json đo lỗi layout. Ảnh 01–12 cùng thư mục; không dùng ảnh lỗi harness làm bằng chứng PASS.

## 3. Defects — chưa sửa

### D1 — Medium: nhãn Phê duyệt bị co/cắt
Repro: 390x844, tiếng Việt, mở plan đang chờ duyệt/blocked, xem bước 2.
Expected: vòng tròn chỉ chứa số; nhãn đầy đủ.
Actual: nhãn nằm trong ô 25x25 px, nội dung cần 29x33; xuống dòng và trạng thái current chỉ thấy phần Phê.
Evidence: 09-blocked-mobile-vi.png,10-progress-label-vi.png,visual-probe.json.
Source: src/main/resources/templates/home.html:370; static/app.css:102 áp dụng vòng tròn cho cả hai span, overflow ở :264.
Requirement NFR-UX01/UX02: quyền hạn và nhãn phải rõ.

### D2 — Medium: toast che mode/state trên mobile
Repro: 390x844, VI, bật Emergency Stop, quan sát ngay khi toast xuất hiện.
Expected: quyền hạn/trạng thái luôn đọc được.
Actual: toast y80 cao68 che mode/state và một phần hàng tab; đo giao nhau với cả hai vùng. Tự tắt khoảng4s, không phải treo; không khẳng định chặn click vì pointer-events:none.
Evidence:11-toast-header-overlap-vi.png,visual-probe.json.
Source:src/main/resources/static/app.css:91; app.js:1698.
Requirement NFR-UX01.

### D3 — Low: tác vụ nội bộ dùng hướng dẫn học phí
Repro: tạo low-risk plan250.000VND vào Emergency Fund, bật Stop, mở review.
Expected: hướng dẫn theo chuyển nội bộ và nguyên nhân dừng.
Actual: lý do AGENT PAUSED đúng nhưng hướng dẫn quay lại hóa đơn/làm mới quote; Emergency Fund nằm trong panel trường/nhà cung cấp giáo dục.
Evidence:09-blocked-mobile-vi.png.
Source:src/main/resources/templates/home.html:378,401,404.
Requirement cleanup: giải thích dễ hiểu/phù hợp tác vụ. Chưa thấy thực thi sai.

## 4. Phần chưa kiểm chứng

- Công cụ browser tương tác BLOCKED vì lỗi khởi tạo.
- Điện thoại thật/bàn phím OS/touch chưa kiểm chứng. Thu nhỏ viewport không phải test bàn phím thật.
- Chưa xác nhận SHA Live của Render; không tác động workspace chung.
- Gemini/quota/timeout hosting ngoài phạm vi; LLM tắt. Không khẳng định mọi bản dịch đạt, chỉ nhãn/trạng thái luồng đã chạy.
- Không full suite/race hai phiên trong đợt này; idempotent retry đã chạy UI và test backend mục tiêu.

## 5. Kết luận và checklist tối thiểu

GO có điều kiện cho demo desktop, CHƯA chốt UI/UX hoàn toàn. Trong các ca đã chạy, chưa quan sát lỗi tài chính/an toàn nghiêm trọng. D1/D2 cần task sửa riêng trước khi đóng responsive; D3 ưu tiên sau. Không sửa trong đợt này, không mở phase AI.
Bạn cần xác nhận SHA Live; kiểm tra điện thoại thật: chat+bàn phím, nhập/sửa/gửi, đóng/mở/cuộn, xem draft/nút phê duyệt. Chỉ xem, không reset/thanh toán Render chung. Không cần lặp toàn bộ các luồng automation.
Bằng chứng ở target là artifact tạm, mvn clean sẽ xóa.
