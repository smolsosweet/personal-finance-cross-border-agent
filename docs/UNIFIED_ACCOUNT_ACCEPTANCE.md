# Nghiệm thu sổ tài khoản chung — 05/10/2026

## Phiên bản và phạm vi

Production được kiểm chứng: **c63a3c6**. Không sửa production code, cấu hình, schema hoặc dependencies trong đợt này. Chỉ thêm test live và lưu bằng chứng. Không push, deploy, reset hoặc thanh toán trên Render; không tác động app local của người dùng.

## Kết quả tám điểm nghiệp vụ

| Kiểm tra | Kết quả | Bằng chứng thực thi |
| --- | --- | --- |
| Cùng tài khoản/số dư giữa Tổng quan, nguồn tiền, chat và payment | PASS | UnifiedAccountLedgerIntegrationTest.allScreensHaveTheSameSixOwnedSourcesAndNeverCountRecipientMoney; UnifiedAccountsPlaywrightTest.sameAccountInventoryAndOneChatAcrossAllViews; Gemini live đối chiếu CHECKING/PAYER_VND cùng Bank A Everyday |
| Tuition trừ đúng và cập nhật các màn hình | PASS | UnifiedAccountsPlaywrightTest.paymentUpdatesOverviewAccountsAndChatWithoutSecondDebit và Gemini browser: 100.000.000 → 29.239.200 VND; tổng tài sản 315.000.000 → 244.239.200 VND |
| Không cộng trùng; tách tiền tệ | PASS | GlobalAssistantIntegrationTest.totalsSeparateCurrenciesAndExcludeRecipientFeeAndPlannerViews; loại SYSTEM recipient; nguồn nhập tay chỉ cộng một lần |
| Chuyển nội bộ bảo toàn tổng tiền, không tăng thu/chi | PASS | UnifiedAccountLedgerIntegrationTest.transfersAcrossOwnedSourcesConserveMoneyAndNeverAddIncomeOrExpense |
| Approval/event trùng không ghi nhận lần nữa | PASS | UnifiedAccountLedgerIntegrationTest.bankEventsAndPaymentsUpdateOneBalanceAndReceiptIsHistorical; Gemini live gửi lại approval, vẫn một sandbox_transactions và số dư không đổi |
| Receipt cũ giữ số dư lịch sử | PASS | Sau salary mô phỏng +900.000: current 30.139.200 VND, receipt vẫn 29.239.200 VND; chat Gemini phân loại đúng hai câu hỏi |
| Planner/runway dùng sổ chung | PASS | UnifiedAccountLedgerIntegrationTest.projectionsReserveOnlyPlansFundedByTheChosenAccount; RunwayIntegrationTest và RunwayPlaywrightTest đạt trong full suite. Projection giữ trước theo tài khoản; runway dùng mức chi sinh hoạt đã xác nhận, không trừ planner thêm lần nữa |
| Tiền mặt có trong tài sản, không tự có quyền thanh toán | PASS | UnifiedAccountLedgerIntegrationTest.manualCashIsSharedButHasNoPaymentPermissionAndArchiveRemovesItEverywhere; browser kiểm tra 7 nguồn với tiền mặt, tổng 317.000.000 VND |

Các tên test là bằng chứng tự động đã chạy, không phải tuyên bố toàn bộ từng điểm đều đã thử thủ công bằng model thật.

## Lệnh và kết quả thực tế

Maven dùng tại máy: `C:\Users\ADMIN\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd`.

```powershell
mvn test
# BUILD SUCCESS: 336 tests, 0 failures, 0 errors, 0 skipped; 4 phút 15 giây.
# 332 test thuộc repository đã commit và 4 ScreenshotEvidenceCollectorTest
# đang có trong workspace từ đợt review UI. Không chỉnh/sử dụng file đó trong commit mới.

# Trong tiến trình riêng đã có GEMINI_API_KEY, key chỉ kế thừa qua environment:
$env:SPRING_DATASOURCE_URL='jdbc:h2:file:./target/unified-gemini-live;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE'
.\scripts\start-gemini.cmd -Port 8152
mvn -Dtest=UnifiedAccountsGeminiLiveIT test
# BUILD SUCCESS: 1 live browser test, 0 failures/errors/skips.
```

Full suite mặc định không chạy các `*LiveIT` opt-in. Các provider/mock test không được tính là gọi Gemini thật. Test live được chạy riêng sau full suite; không cần chạy lại full suite vì chỉ thêm artifact kiểm chứng, không sửa production.

## Gemini thật + Chrome browser

Provider gemini, model gemini-3.5-flash-lite, HTTP local 8152, database nghiệm thu riêng. **5 generation submissions / giới hạn 20**, năm kết quả có intent mong đợi và schema hợp lệ. Không retry để che lỗi. Các thao tác UI/approval/simulated event không gọi model.

| Bước | Kết quả kỳ vọng và thực tế | Độ trễ từ browser |
| --- | --- | --- |
| Hỏi số dư Bank A Everyday, tiếng Việt | EXPLAIN_CURRENT_BALANCE; 100.000.000 VND, chưa có plan/payment | 1.957 ms |
| Yêu cầu draft học phí, tiếng Anh | CREATE_TUITION_PLAN; AWAITING_APPROVAL, một plan, không payment/receipt/debit | 1.298 ms |
| Người dùng bấm approval riêng; gửi lại approval | Một payment duy nhất, tổng trừ 70.760.800 VND; current 29.239.200 VND; Overview/accounts/payment đồng nhất | Không gọi model |
| Hỏi số dư mới, tiếng Anh | EXPLAIN_CURRENT_BALANCE; 29.239.200 VND | 1.453 ms |
| Nhận salary mô phỏng +900.000 VND; hỏi receipt cũ bằng mã giao dịch, tiếng Việt | EXPLAIN_RECEIPT_BALANCE; receipt vẫn 29.239.200 VND, không thanh toán thêm | 1.631 ms |
| Hỏi số dư hiện tại lần nữa, tiếng Anh | EXPLAIN_CURRENT_BALANCE; 30.139.200 VND, một payment duy nhất | 1.641 ms |

Độ trễ trên tương đương khoảng **1,30–1,96 giây**, gồm UI/backend/model, không chỉ latency API. Test so snapshot tài chính trước/sau từng câu hỏi chỉ đọc. Plan ACT-687DBF83-43E; receipt SBOX-3A6DBC4A-B2D — synthetic riêng local.

Bằng chứng bền vững:
- `docs/unified-accounts/full-suite-results.json` — chỉ kết quả class thực sự chạy, không chứa system properties/key.
- `docs/unified-accounts/gemini-live-results.json` — câu hỏi, intent, schema, latency, reply và số plan/payment.
- `docs/unified-accounts/gemini-live-final.png` — Chrome trên local app Gemini thật.
- `src/test/java/com/example/finance/UnifiedAccountsGeminiLiveIT.java` — test opt-in, chỉ dùng localhost:8152 và database riêng. Không chạy lại trên DB đã có dữ liệu/count cũ.

## Giao diện và giới hạn còn lại

Các danh sách dài bị giới hạn chiều cao và cuộn trong vùng. Summary, cảnh báo và approval không bị ép chiều cao cố định. Chrome desktop/mobile viewport, focus và overflow có test đã chạy. **Điện thoại thật: chưa kiểm tra**; không tuyên bố PASS cho bàn phím/scroll trên thiết bị thật.

**Render: chưa nghiệm thu c63a3c6 trong đợt này**, chưa push/deploy. Kết quả local không chứng minh bản cloud đã đạt. Sau deploy vẫn cần xác nhận SHA Live và smoke ngắn có phối hợp nếu thay đổi dữ liệu chung.

Không phát hiện lỗi mới trong các ca đã thực thi. Đủ bằng chứng local cho tám điểm nghiệp vụ và gate full suite/Gemini. Demo vẫn synthetic dùng chung hồ sơ, chưa phải production với tài khoản và tiền thật.
