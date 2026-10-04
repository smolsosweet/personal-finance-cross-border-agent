# Khởi động local một lệnh và sửa vị trí form trợ lý

Kiểm chứng trên Windows, ngày 04/10/2026 (+07). Baseline Task B: `0fa79ee`.
Hai phạm vi được thực hiện riêng theo yêu cầu trực tiếp và file đính kèm:
UX form sinh hoạt và công cụ khởi động local. Không push, deploy hoặc bắt đầu phase khác.

## 1. File thay đổi

Đường dẫn tương đối với `D:\personal-finance-cross-border-agent`.

### UX — commit riêng `89a6f58`

| File | Thay đổi |
| --- | --- |
| `src/main/resources/templates/home.html` | Đưa form runway ra ngoài vùng lịch sử cuộn, ngay trên ô chat; phần giải thích có thể mở thêm. |
| `src/main/resources/static/app.css` | Dock gọn: số tiền và VND cùng hàng, nút xác nhận/xóa; lịch sử có vùng cuộn riêng. |
| `src/main/resources/static/app.js` | Dịch nhãn giải thích mới; không sửa business rule hoặc contract AI. |
| `src/test/java/com/example/finance/RunwayPlaywrightTest.java` | Ca hồi quy với lịch sử dài, desktop/mobile, form tiếp cận được mà không cuộn ngược lịch sử; snapshot tài chính nguyên trạng. |

### Startup — commit cục bộ riêng

| File | Thay đổi |
| --- | --- |
| `src/main/resources/application-local.properties` | Profile opt-in: Ollama/qwen3:4b, enabled, localhost:11434, connect 3s/request 60s; giữ FINBRIDGE_LLM_* overrides khi dùng profile trực tiếp. |
| `scripts/start-local.ps1` | Kiểm tra Java/Maven, model/Ollama, port; process riêng, logs trực tiếp, readiness có timeout, warmup, Ctrl+C và cleanup đúng cây process. |
| `scripts/prepare-demo.ps1` | Giữ warmup model weights cũ; thêm `-FinBridgeUrl` gọi intent chỉ đọc thật, dùng session mới, phân biệt fallback/clarification với thành công. Dùng chung preflight với launcher. |
| `README.md` | Prerequisites, một lệnh, tham số, lỗi, DB, IntelliJ và giới hạn local/cloud. |
| `src/test/powershell/LocalStartup.Tests.ps1` | 23 checks cú pháp, prerequisites, wrapper, port owner, failure/timeout cleanup, warmup fallback, skip và browser bằng stub có giới hạn. |
| `src/test/java/com/example/finance/LocalStartupScriptTest.java` | Tích hợp checks PowerShell 5.1 vào Maven trên Windows. |
| `src/test/java/com/example/finance/LocalStartupLiveIT.java` | Opt-in: gọi warmup từ PS 5.1 vào app thật và đối chiếu mọi bảng tài chính trên DB target/ riêng. |
| `docs/LOCAL_STARTUP_VERIFICATION.md` | Báo cáo này. |

## 2. Cách khởi động

Dừng app cũ bằng Ctrl+C trong terminal đã mở app đó. Từ thư mục project, dùng PowerShell:

```powershell
.\scripts\start-local.ps1
.\scripts\start-local.ps1 -Port 8081 -OpenBrowser
.\scripts\start-local.ps1 -SkipWarmup
.\scripts\start-local.ps1 -StartupTimeoutSeconds 180
# Từ thư mục khác:
& 'D:\personal-finance-cross-border-agent\scripts\start-local.ps1' -Port 8081
```

Lần đầu cần JDK 21/Maven 3.9+/Ollama và `ollama pull qwen3:4b`.
Script không tự cài phần mềm, tải model, dừng process đang chiếm port, đổi firewall hoặc mở Ollama ra Internet.
Không mở browser mặc định. Ctrl+C dừng cây Maven/app mà script tạo, giữ Ollama và app khác.
Override được đặt vào environment của process con và tham số Spring, không sửa environment người dùng/máy.
Script không đổi working directory của caller; Maven luôn chạy từ project root.

Profile `local` không bật toàn cục. Chạy `mvn spring-boot:run` thông thường giữ hành vi cũ.
IntelliJ có thể đặt active profile `local`; script launcher cố định provider/model dù env kế thừa xung đột.

## 3. Lệnh kiểm chứng và kết quả thực tế

### Chrome cho UX form

```powershell
mvn '-Dtest=RunwayPlaywrightTest,AssistantPanelPlaywrightTest,ContextualConversationPlaywrightTest' test
```

**14/14 đạt**, 0 fail/error/skip, 47,647 giây. Chrome thật, model mock.
Ca mới tạo lịch sử dài qua tám lượt follow-up, kiểm tra form ngoài `.assistant-body`,
input/nút xác nhận nằm trong panel/dock ở 1440 và 390 px, xác nhận lại 9 triệu và snapshot không đổi.
Ảnh `target/runway-docked-mobile.png` đã được đọc kiểm tra: form và ô chat cùng hiện trên mobile.

### Regression và Qwen thật qua launcher

App riêng đã được launcher khởi động trước, dùng DB `target/local-launcher-verification-ps51`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn '-Dtest=LocalStartupScriptTest,LocalStartupLiveIT,OllamaIntentClientTest,StrictLlmIntentParserTest,PersonalFinanceAiIntegrationTest' '-Dfinbridge.startup.test.url=http://localhost:8113' '-Dfinbridge.startup.test.jdbc-url=jdbc:h2:file:./target/local-launcher-verification-ps51;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE' test

powershell.exe -NoProfile -ExecutionPolicy Bypass -File src/test/powershell/LocalStartup.Tests.ps1
# PowerShell 7 có sẵn trong môi trường kiểm chứng:
& 'C:\Users\ADMIN\.cache\codex-runtimes\codex-primary-runtime\dependencies\native\powershell\pwsh.exe' -NoProfile -File src/test/powershell/LocalStartup.Tests.ps1
```

**22/22 đạt**, 0 fail/error/skip, 39,670 giây. Gồm:
5 Ollama client + 4 strict parser + 11 Personal Finance integration + 1 script test + 1 live test.
Script checks **23/23 đạt trên PS 5.1 và PS 7**. Checks lỗi prerequisites/provider/browser dùng stub;
không coi chúng là lời gọi model thật. Test live không mock model: audit intent thực tế
`EXPLAIN_BUDGET_STATUS`, schema đã qua strict parser, snapshot tài chính nguyên trạng;
plans 0, approvals 0, payments 0, ledger 0. Warmup lần cuối **4,662 giây**.
Lượt cold trước đó thành công trong **35,985 giây** với cùng kiểm tra snapshot.

### Khởi động, môi trường xung đột và Ctrl+C thực tế

Để tránh DB người dùng, các lần probe đặt `SPRING_DATASOURCE_URL` ở process kiểm chứng thành
H2 file dưới `target/`; launcher giữ override DB này. Không dùng `/reset`.

```powershell
# Chạy từ C:\Users\ADMIN, với env LLM false/disabled/model khác và SPRING_APPLICATION_JSON profile postgres:
& 'D:\personal-finance-cross-border-agent\scripts\start-local.ps1' -Port 8112 -SkipWarmup
& 'D:\personal-finance-cross-border-agent\scripts\start-local.ps1' -Port 8113
# Probe wrapper chỉ đặt env/DB kiểm chứng và gọi cùng script; in rõ runtime 5.1.19041.7725:
powershell.exe -NoProfile -ExecutionPolicy Bypass -File 'D:\personal-finance-cross-border-agent\target\start-local-ps51-probe.ps1'
# Trong probe, lệnh khởi động thực tế là:
& 'D:\personal-finance-cross-border-agent\scripts\start-local.ps1' -Port 8114
# Thử port người dùng đang dùng:
.\scripts\start-local.ps1 -Port 8080
```

| Check yêu cầu | Quan sát | Trạng thái |
| --- | --- | --- |
| Ollama reachable/qwen3:4b có sẵn | `/api/tags` trả model trước/sau launch và stop. | Đạt — thật |
| App reachable, local/Ollama | HTTP 200; logs active profile local; warmup budget qua model thật dù inherited settings disabled/openai/model khác. | Đạt — thật |
| Warmup thực tế, chỉ đọc | 8113 tự warmup 4,809 giây; PS 5.1 ở 8114 tự warmup 4,754 giây. SQL snapshot trước/sau warmup live bằng nhau. | Đạt — thật |
| Ctrl+C cleanup | Gửi Ctrl+C qua PTY cho từng phiên. 8112/8113/8114 đều được giải phóng; PID app 27724/28916/30888 đã dừng. | Đạt — thật |
| Giữ process khác | Listener 8080 của app cũ còn; Ollama `/api/tags` còn reachable. | Đạt — thật |
| Ngoài working directory repo | Khởi động từ C:\Users\ADMIN, logs/H2 file xác nhận app cwd là project root. | Đạt — thật |
| Port đang chiếm | Port 8080 báo occupied, exit 1; owner vẫn giữ port. Stub TcpListener cũng còn connect được sau reject. | Đạt — thật + stub |
| Ollama unavailable/model missing | Stub connection failure → hướng dẫn Ollama/ollama serve; missing model → ollama pull qwen3:4b; không khởi tạo app trước preflight. | Đạt — stub, không tắt Ollama thật |
| SkipWarmup | 8112 báo SKIPPED/AI chưa xác minh; stub xác nhận không có warmup POST. | Đạt — thật + stub |
| Warmup fallback | Stub app tiếp tục chạy tới khi tự exit 0, có nhãn FAILED; không giả thành AI-ready. | Đạt — stub |
| Browser tùy chọn | Default không gọi mở browser, true gọi đúng một lần. Không mở browser native trong checkpoint. | Đạt — stub |
| Exit sớm/timeout | Fake owned Maven exit 7 → launcher trả 1; timeout 10 giây → trả 1 và owned tree biến mất. Cwd/env caller nguyên trạng. | Đạt — process thật, app/HTTP stub |

Một phép kiểm tra DOM sơ bộ dùng `data-transaction-id` cho receipt cho kết quả false negative,
vì attribute này cũng xuất hiện ở hàng lịch sử giao dịch seed. Không coi phép đó là đạt.
Đối chiếu đúng bằng SQL sau phiên PS 5.1: action_plans 0, approvals 0,
sandbox_transactions 0, sandbox_ledger_entries 0; audit có ba budget classifications
(hai warmup tự động và một live regression). Không có bằng chứng thanh toán được tạo.

Lần test phát triển đầu tiên thất bại do Get-Command mvn trả nhiều launcher và biến fixture
`$port` xung đột với parameter int khi dot-source. Đã sửa chọn `mvn.cmd` duy nhất và đổi tên
biến test. Các số đạt ở trên là lần chạy cuối. Không rewrite kỳ vọng để bỏ qua lỗi.

## 4. File bảo vệ và dữ liệu

Lệnh đã chạy, exit 0:

```powershell
git diff --exit-code 0fa79ee -- src/main/java src/main/resources/schema.sql src/main/resources/application.properties src/main/resources/application-postgres.properties pom.xml Product_Requirements_Personal_Finance_Cross_Border_Agent.docx
git diff --check
```

Mọi production Java, strict AI contract/parser, Policy Guard, approval/execution,
Payment Sandbox, FX/fee, schema, dependency và PRD nguyên trạng. Ngoài UX được yêu cầu,
startup chỉ thêm profile opt-in; default/test/cloud properties không đổi.

Database mặc định là H2 file `finance-phase1` trong project root, không phải H2 memory.
Launcher không gọi reset và không thay đổi initialization. Hành vi cũ: seed bảng trống,
repair/reset fixture demo không tương thích, refresh quote hết hạn, update runtime ONLINE/OFFLINE
trên startup. Reset UI vẫn là thao tác riêng. Nếu dùng H2 memory trong test, dữ liệu mất khi JVM dừng.
Đổi port không tách DB; hai instance cùng dùng file mặc định sẽ chia sẻ dữ liệu.

## 5. Known issues và hoàn tất

- Không có blocker trong các check bắt buộc trên Windows. UX đã commit riêng;
  startup đủ điều kiện commit cục bộ `chore(dev): add one-command local Ollama startup`.
- Ctrl+C qua PTY kiểm chứng trả exit 1 ở host PowerShell; cleanup và port release đạt.
  Exit code khi host ngắt pipeline có thể khác return code thông thường của script.
- Cleanup cây process dùng taskkill /T /F cho đúng PID launcher tạo; đây là dừng cưỡng bức,
  không phải endpoint graceful shutdown mới. Không có real money/API trong demo.
- `-OpenBrowser` được kiểm tra bằng stub; không claim đã thao tác cửa sổ browser native.
- Warmup PASS chỉ chứng minh câu hỏi ngân sách hỗ trợ tại thời điểm kiểm chứng; không bảo đảm
  mọi câu hỏi/model latency. FAILED/SKIPPED không tuyên bố AI-ready.
- Không chạy lại full suite không liên quan: checkpoint này chạy 14 Chrome UX + 22 test startup/AI
  liên quan, cùng 23 checks trên hai PowerShell. Full suite 204/204 thuộc checkpoint Task B trước đó.
- Logs/DB/probe/screenshots trong `target/` là artifacts ignored, không commit DB hoặc log.
- Local profile không kết nối cloud deployment tới Ollama trên laptop.

Đã dừng toàn bộ app do checkpoint này tạo; app người dùng ở 8080 và Ollama được giữ nguyên.
Không push hoặc bắt đầu phase tiếp theo.
# Bổ sung launcher CMD — 04/10/2026

- Từ CMD: `cd /d D:\personal-finance-cross-border-agent`, rồi `scripts\start-local.cmd`.
- Mở Ollama từ Start Menu trước; model qwen3:4b đã tải thì không cần pull lại. Launcher không tự mở Ollama.
- File CMD tự gọi Windows PowerShell với execution policy chỉ áp dụng cho process, chuyển tiếp tham số và exit code. Không dùng liên kết mở file .ps1 của Windows nên không mở Notepad.
- `mvn -Dtest=LocalStartupCmdTest,LocalStartupScriptTest test`: **2/2 đạt**. Test script có 23 kiểm tra nội bộ; test CMD thực thi wrapper, chuyển đúng port, phát hiện port bận trước khi chạy app và trả exit code 1.
- Chạy thật bằng `cmd.exe /d /c scripts\start-local.cmd -Port 8120`, với `SPRING_DATASOURCE_URL=jdbc:h2:mem:cmd_launcher_verify;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1` chỉ trong môi trường process kiểm chứng.
- HTTP readiness đạt; local/Ollama/qwen3:4b đúng; warmup qua câu hỏi ngân sách thật **PASS trong 37,687 giây**. Không reset database người dùng hoặc thực hiện thanh toán.
- Ctrl+C đã dừng listener 8120; CMD hỏi `Terminate batch job (Y/N)?`, nhập Y để kết thúc batch. Ollama 11434 vẫn chạy.
- Chỉ chỉnh entry point và hướng dẫn chạy; không đổi logic tài chính hoặc tích hợp AI.
