# Personal Finance Cross Border Agent — Guarded AI Intent Providers

The `main` branch implements the deterministic demo through Phase 5 plus an optional guarded intent classifier using OpenAI, local Ollama, or Gemini.

- Phase 1: synthetic user, bank events, normalization and transaction type detection.
- Phase 2: merchant categorization, confidence handling, Undo, dashboard, budgets and Proactive Feed.
- Phase 3: fixed Vietnam to China tuition corridor, School Registry verification, eligible channel comparison and deterministic landed cost.
- Phase 4: conversation UI, structured action plans, Approval Mode, Delegated Mode for low-risk actions, deterministic Policy Guard, multi-currency Payment Sandbox, receipts, idempotency, Audit Log and Emergency Stop.
- Phase 5: prompt-injection, recipient, channel, FX, deadline, limit and safe-balance guards, offline fallback, reset/replay and the five-minute demo acceptance flow.

All data and payments are synthetic. No real Alipay, bank or payment provider is connected. When enabled,
the selected LLM provider classifies supported personal-finance and tuition intents through strict structured output. It cannot execute
payments, modify policy, invent rates or fees, or change a recipient after approval.

## Run

Requirements: Java 21 and Maven 3.9+.

### Gemini API trên Windows — không cần Ollama

Nếu `GEMINI_API_KEY` đã có trong terminal, không nhập lại key. Từ folder project,
chạy một lệnh (dùng được trong CMD và PowerShell, kể cả khi Windows chặn `.ps1`):

```bat
scripts\start-gemini.cmd
```

Trong PowerShell cho phép script, lệnh tương đương là ` .\scripts\start-gemini.ps1`.
Không mở `.ps1` bằng nhấp đúp. File `.cmd` gọi PowerShell với ExecutionPolicy Bypass
chỉ cho tiến trình con; không thay đổi chính sách toàn máy.

Nếu chưa có key trong terminal, mở PowerShell và nhập kín ở máy của bạn:

```powershell
cd D:\personal-finance-cross-border-agent
$geminiInput = Read-Host 'Gemini API key (nhap kin, khong gui vao chat)' -AsSecureString
$env:GEMINI_API_KEY = [System.Net.NetworkCredential]::new('', $geminiInput).Password
Remove-Variable geminiInput
.\scripts\start-gemini.cmd
```

Key chỉ tồn tại trong môi trường của terminal và các tiến trình con. Một cửa sổ CMD
mở riêng từ Start Menu không kế thừa biến `$env:` của PowerShell đang chạy. Có thể
chạy `.cmd` ngay trong PowerShell như trên hoặc mở `cmd.exe` từ PowerShell đó.
Không đặt key trong source, README, `.properties`, URL, frontend, Git hoặc tham số CLI.

Chờ `Application ready: http://localhost:8080` rồi mở URL, giữ terminal mở.
**Khởi động gửi 0 yêu cầu model**, nên `AI readiness: NOT VERIFIED` là bình thường.
Gửi một câu hỏi được hỗ trợ mới gọi Gemini; hệ thống không warmup, retry hoặc đổi provider.
Ctrl+C dừng cây tiến trình Maven/app do launcher tạo, không dừng ứng dụng khác.
Port đang dùng sẽ bị từ chối, không tự kill process.

```bat
scripts\start-gemini.cmd -Port 8081 -OpenBrowser
scripts\start-gemini.cmd -Model gemini-3.5-flash-lite -ConnectTimeoutSeconds 3 -RequestTimeoutSeconds 30
```

Launcher chọn rõ `gemini` / profile `gemini` / model `gemini-3.5-flash-lite`, ghi đè
cấu hình provider/model cũ chỉ trong tiến trình con. Có thể dùng `-StartupTimeoutSeconds 180`.
Nó chỉ cần Java 21 và Maven 3.9+, không kiểm tra/cài/tải Ollama, không reset dữ liệu.
Chạy trực tiếp qua Maven có thể dùng profile `gemini`, `FINBRIDGE_LLM_ENABLED`,
`FINBRIDGE_LLM_MODEL`, `FINBRIDGE_LLM_CONNECT_TIMEOUT`, `FINBRIDGE_LLM_REQUEST_TIMEOUT`.
`GEMINI_API_KEY` được bind vào `finbridge.llm.gemini-api-key`, tách khỏi `OPENAI_API_KEY`.

**Model và tài liệu Google kiểm tra ngày 2026-10-04:**

- [Gemini 3.5 Flash-Lite](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite): stable, structured outputs supported.
- [Pricing](https://ai.google.dev/gemini-api/docs/pricing): standard input/output có free tier; quota/quyền truy cập phụ thuộc project/key và có thể đổi.
- [generateContent REST](https://ai.google.dev/api/generate-content): POST `/v1beta/models/{model}:generateContent`, `systemInstruction`, `contents`, `generationConfig.responseMimeType=application/json` và `responseJsonSchema` dùng schema chung đóng bốn trường.
- [API key header](https://ai.google.dev/gemini-api/docs/api-key): `x-goog-api-key`, không dùng query parameter.
- [Structured output](https://ai.google.dev/gemini-api/docs/structured-output): vẫn kiểm tra JSON lại bằng `StrictLlmIntentParser`; không tin output model chỉ vì có schema.

FinBridge không bật billing, mua credits, dùng grounding/tools trả phí hoặc tự chọn model khác.
Model có free tier không chứng minh key/project của bạn còn quota hay đang ở tier miễn phí;
cần xác nhận tier trong AI Studio của bạn. Chỉ dùng dữ liệu tổng hợp cho đợt kiểm chứng.
Backend gửi câu hỏi hiện tại và ngữ cảnh enum tối thiểu, không gửi lịch sử, số tài khoản,
số dư hay bill/quote từ database. Câu trả lời và các phép tính vẫn do backend dựng.

Key/model thiếu làm startup thất bại với thông báo cấu hình. Lỗi 401/403, 404, 429,
timeout, 5xx, blocked/truncated/invalid output trả fallback an toàn kèm mã `GEMINI_*`
trong Audit Log. Không tạo plan từ keyword fallback, không thanh toán, không retry.
Luồng học phí có hướng dẫn vẫn dùng được; học phí luôn cần phê duyệt riêng.
Thông báo không chứa raw provider body/header/key. Deployment tương lai phải dùng
**server-side environment secrets**; phase này chưa deploy hay thêm authentication.

Quay lại Ollama: dừng app bằng Ctrl+C, rồi chạy `scripts\start-local.cmd` với Ollama
đã mở và `qwen3:4b` đã tải. `application-local.properties` và launcher cũ được giữ nguyên.

#### Test Gemini (mock và live tách riêng)

```bat
mvn "-Dtest=GeminiIntentClientTest,GeminiIntegrationTest,LlmProviderConfigurationTest,GeminiStartupScriptTest" test
mvn test
```

Các test mặc định dùng HTTP stub (và Chrome thật ở `GeminiIntegrationTest`), không tiêu quota.
Live test chỉ chạy khi được chọn rõ, cần Chrome có sẵn và app Gemini đang chạy bằng
database tổng hợp riêng; không dùng database demo thường:

```powershell
# Terminal có key; khởi động app riêng, không gửi chat thủ công trong lúc smoke chạy:
$env:SPRING_DATASOURCE_URL='jdbc:h2:file:./target/gemini-live-check;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE'
.\scripts\start-gemini.cmd -Port 8122
```

Ở terminal khác trong project:

```bat
mvn "-Dtest=GeminiLiveIT" test
```

Live test đọc snapshot database synthetic, kiểm tra provider/model qua metadata tiến trình,
chạy Chrome thật, EN/VI, follow-up, Bank B, runway có xác nhận, ambiguity, injection
prefilter và draft `AWAITING_APPROVAL`. Không phê duyệt/thực thi/reset.
Tối đa 20 submission qua bộ đếm `target/gemini-live-generation-count.txt` (tính cả lỗi,
không tự xóa khi chạy lại); injection bị prefilter không được tính là model trả đúng.
Test fail/blocked phải được báo riêng; startup thành công hay test mock không chứng minh Gemini thật hoạt động.
Nếu Windows không cho đọc command line của terminal khác, cần log khởi động **đã quan sát**
chứa `PID=...`, dòng `Application ready: ...` và `Profile: gemini | Provider: gemini | Model: gemini-3.5-flash-lite`
trong `target/gemini-live-startup-evidence.txt` (không chứa key). Test đối chiếu PID với port
đang listen và ghi rõ dùng log thay vì command-line metadata. Không tự mặc định provider.
Nếu đã chạy thành công các intent đầu rồi dừng trước xác nhận runway, có thể chọn riêng phần
còn lại bằng `mvn "-Dtest=GeminiLiveIT" "-Dgemini.live.resume=true" test`; bộ đếm vẫn giữ nguyên.
Hỏi Bank B sẽ giữ ngữ cảnh kênh không khả dụng; chọn lại hóa đơn/kênh hợp lệ trong UI trước
khi chạy runway. Test dùng thao tác UI này, không bỏ kiểm tra eligibility.
Sau smoke, dừng terminal app. Bỏ `SPRING_DATASOURCE_URL` kiểm chứng ở terminal đó trước
khi quay lại demo thường: `Remove-Item Env:SPRING_DATASOURCE_URL`. Không cần bỏ key.

### Chạy từ CMD trên Windows (khuyến nghị)

Model `qwen3:4b` đã tải thì không cần tải lại. Mỗi lần chạy:

1. Mở **Ollama** từ Start Menu và để ứng dụng chạy ở nền.
2. Nếu FinBridge cũ đang chạy, ở cửa sổ đó nhấn **Ctrl+C**; nếu hỏi `Terminate batch job (Y/N)?`, nhập `Y` rồi Enter.
3. Mở **Command Prompt (CMD)** và chạy từng dòng:

```bat
cd /d D:\personal-finance-cross-border-agent
scripts\start-local.cmd
```

Không mở file `.ps1` bằng nhấp đúp và không dùng cú pháp `$env:...` trong CMD.
File `.cmd` tự gọi PowerShell, bật Ollama cho FinBridge, kiểm tra model và làm nóng AI.
Không cần nhập các biến môi trường hoặc chạy `mvn` riêng. Script không tự mở Ollama.

Chờ dòng `Application ready: http://localhost:8080`, sau đó kết quả `Warmup: PASS`.
Lượt làm nóng model có thể mất tới 60 giây. Mở **http://localhost:8080** và nhấn **Ctrl+F5** để nạp giao diện mới; giữ CMD mở khi dùng web.
Nếu `Warmup: FAILED`, web vẫn có thể chạy nhưng chưa xác nhận AI thật sẵn sàng; đọc lỗi phía trên.

| Thông báo | Cách xử lý trong CMD |
| --- | --- |
| `Ollama is unavailable` | Mở ứng dụng Ollama rồi chạy lại. Nếu không dùng ứng dụng desktop, chạy `ollama serve` ở một CMD riêng và giữ cửa sổ đó mở. |
| `Model qwen3:4b is not installed` | Chạy `ollama pull qwen3:4b` một lần, chờ tải xong rồi chạy lại script. |
| `Port 8080 is already occupied` | Dừng app cũ bằng Ctrl+C, hoặc chạy `scripts\start-local.cmd -Port 8081` và mở http://localhost:8081. |
| Java/Maven missing | Kiểm tra `java -version` và `mvn -version`; cần JDK 21, Maven 3.9+. Mở CMD mới sau khi sửa PATH. |
| `address already in use` khi chạy `ollama serve` | Ollama đã có server chạy. Không mở thêm server; chạy script FinBridge. |

Có thể tự kiểm tra model đã tải bằng `ollama list`; cần có `qwen3:4b`.
Ollama chạy model tại cổng **11434**, còn web FinBridge chạy tại **8080** (hoặc port bạn chọn).

### One-command local Ollama startup (Windows PowerShell 5.1 / PowerShell 7)

First install **JDK 21**, **Maven 3.9+**, and **Ollama**. Ensure `java -version` and
`mvn -version` work in a new PowerShell terminal. If `JAVA_HOME` is set, it must point to
the JDK folder, not `bin`. This checkout currently has no Maven wrapper; the launcher
prefers `mvnw.cmd` if one is present, otherwise it uses installed `mvn.cmd`.
Start the Ollama app, then download the model once:

```powershell
ollama pull qwen3:4b
```

From the project folder, use one command for subsequent runs:

```powershell
.\scripts\start-local.ps1
```

The launcher checks prerequisites and port 8080, starts profile `local` with
`ollama / qwen3:4b / http://localhost:11434`, waits up to 120 seconds for the existing home
endpoint, then warms the **actual FinBridge intent path** with a read-only budget question.
Connect timeout is 3 seconds and model request timeout is 60 seconds.
Application logs remain visible. It does not create a payment plan, approve, pay, or reset demo data.
When successful, it prints the URL and live warmup result. **Ctrl+C** stops only the
Maven/application process tree created by this launcher; Ollama and other app instances remain running.
The launcher stays attached until the application stops. It never installs tools or downloads models.

Parameters:

| Parameter | Behavior |
| --- | --- |
| `-Port 8081` | Choose another application port; default 8080. |
| `-SkipWarmup` | Skip the budget request, but still check Ollama and the installed model. AI readiness is unverified. |
| `-OpenBrowser` | Open the local application after readiness; no browser opens by default. |
| `-StartupTimeoutSeconds 180` | Change the bounded readiness timeout (10–600 seconds). |

```powershell
.\scripts\start-local.ps1 -Port 8081 -OpenBrowser
.\scripts\start-local.ps1 -SkipWarmup
# From another directory:
& 'D:\personal-finance-cross-border-agent\scripts\start-local.ps1' -Port 8081
```

If PowerShell blocks unsigned local scripts, a process-only alternative is:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

Troubleshooting:

- **Ollama unavailable:** open the Ollama desktop app, or run `ollama serve` in a separate
  terminal when no Ollama server is running. Keep port 11434 local.
- **Model missing:** run `ollama pull qwen3:4b`, then retry the launcher.
- **Port occupied:** stop the owner yourself or use `-Port 8081`; the launcher never kills its owner.
- **Warmup FAILED:** the application stays available, but AI may be in fallback or need clarification.
  Inspect the assistant/Audit Log; do not treat the application URL alone as proof of live AI readiness.
- **Warmup PASS:** the fresh warmup session received the validated budget template through the real
  application classifier. It is a readiness check, not a guarantee for every prompt. Cold requests can
  be much slower than warm requests; timeout/fallback remain enabled.
- **Startup failure/timeout:** inspect the visible logs. Only this launcher's process tree is cleaned up.

The script sets configuration only in its child process and uses explicit Spring command-line
properties. Inherited LLM settings or `SPRING_APPLICATION_JSON` cannot silently disable AI or select
another provider. Caller environment and working directory remain unchanged. The `local` profile
alone supports the existing `FINBRIDGE_LLM_*` overrides; the launcher intentionally fixes the local
provider/model above. Neither activates globally or changes the default/test/cloud profile.

**Database:** the default is persistent H2 at `finance-phase1` under the project root, including when
launched from another folder. The script adds no reset or database configuration. Existing startup
initialization seeds empty tables, repairs incompatible demo fixtures, refreshes expired quotes, and
updates policy runtime mode; this existing behavior is unchanged. Reset demo is destructive to
synthetic demo changes and remains a separate user action. If an existing test/override selects
`jdbc:h2:mem:...`, those records are lost when that JVM stops. HTTP conversation/scenario state is
session-scoped and is not a permanent financial profile.
Changing `-Port` does not create a separate database: instances using the default H2 file share data.
Stop the previous instance for a normal single-instance demo run.

**IntelliJ:** activate Spring profile `local` in the run configuration, or add
`--spring.profiles.active=local` to program arguments. Start Ollama separately. For the same warmup
on the running app, use `.\scripts\prepare-demo.ps1 -FinBridgeUrl http://localhost:8080`.
This configuration is for development/demo; a cloud deployment cannot reach your laptop's
Ollama by using its own `localhost:11434`.

### Default guided mode

```powershell
mvn test
mvn spring-boot:run
```

Open http://localhost:8080. The default database is a local H2 file. PostgreSQL remains available through the `postgres` Spring profile.

If Maven is not on `PATH`, use the full commands in [PHASE_5_VERIFICATION.md](PHASE_5_VERIFICATION.md).

## Five-minute demo

1. Review the seeded personal transaction and its automatic category.
2. Read the deterministic tuition insight and School Registry verification.
3. Compare Alipay Student Payment and Bank A International Transfer; Bank B is unavailable and cannot be executed.
4. Select a verified active bill and a connected channel, then Create plan opens its contextual payment review. Check beneficiary bank details, funding source, landed cost, remaining balance and quote countdown before approving.
5. Review the receipt and transaction ID for that exact plan. Open its Audit Log or reopen a paid bill through View receipt; paid bills cannot create another payment.
6. Expand Demo tools and policy settings to test malicious instructions, low-risk Delegated actions and offline fallback. Emergency Stop remains in the global header; reset/replay starts from the Dashboard.

Tuition always requires Approval Mode, including when the general permission mode is Delegated. All financial calculations use Java `BigDecimal`.

## Verification

Run the full automated suite and the manual acceptance script described in [PHASE_5_VERIFICATION.md](PHASE_5_VERIFICATION.md). The manual script starts from reset data and verifies all ten acceptance steps plus offline fallback and reset/replay.

## Assistant access

Use **Ask FinBridge** at the bottom-right of any tab. The side panel shares the existing Overview conversation, supports English/Vietnamese, and keeps drafts while switching tabs. Student finance and payment review also have contextual help buttons. Suggested questions are submitted only after Send; a payment draft must still be reviewed and explicitly approved on the payment screen.

See [the assistant UI guide and targeted test results](docs/ASSISTANT_WORKSPACE.md). The existing `APP_DEMO_TOOLS_ENABLED=false` option hides demo tools while retaining the assistant.

## Contextual payment workflow

The payment screen opens after a plan is created. Its history entry then becomes available for reopening plans. Bill/beneficiary/quote snapshots remain immutable; identical pending requests reuse a plan, replacement plans revoke old approvals, and every execution rechecks Policy Guard. Unexecuted plans can be canceled; completed payments keep their original receipt.

See [the workflow and targeted verification report](docs/PAYMENT_WORKFLOW.md) for the Vietnamese guide, exact test commands, observed results and verification limitations.

## Guarded AI intent configuration

The optional AI intent boundary is disabled by default. Provider selection is explicit; an API key by itself never enables a provider.

### Local Ollama with qwen3:4b

Install and start Ollama locally, then check and download the required model:

```powershell
ollama --version
ollama list
ollama pull qwen3:4b
```

Start FinBridge with Ollama:

```powershell
$env:FINBRIDGE_LLM_ENABLED="true"
$env:FINBRIDGE_LLM_PROVIDER="ollama"
$env:FINBRIDGE_LLM_MODEL="qwen3:4b"
$env:FINBRIDGE_LLM_BASE_URL="http://localhost:11434"
$env:FINBRIDGE_LLM_CONNECT_TIMEOUT="3s"
$env:FINBRIDGE_LLM_REQUEST_TIMEOUT="60s"
mvn spring-boot:run
```

`FINBRIDGE_LLM_MODEL` may be omitted for Ollama; it defaults to `qwen3:4b`. Keep Ollama bound locally and do not expose port 11434 publicly.

Check the local service and installed models without sending a prompt:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:11434/api/tags
```


With FinBridge running on port 8089 using the configuration above, run the opt-in browser smoke test that calls the real local model:

```powershell
mvn "-Dtest=OllamaLivePlaywrightIT" test
```

This live test is intentionally excluded from the default Maven test suite so normal builds do not depend on a local Ollama process.

### OpenAI

The existing OpenAI setup remains compatible. `openai` is the default provider value, but the enable flag, model and API key are still required:

```powershell
$env:OPENAI_API_KEY="<set locally; never commit>"
$env:FINBRIDGE_LLM_PROVIDER="openai"
$env:FINBRIDGE_LLM_MODEL="<a Structured Outputs capable model>"
$env:FINBRIDGE_LLM_ENABLED="true"
mvn spring-boot:run
```

To force the guided deterministic experience without any model request:

```powershell
$env:FINBRIDGE_LLM_PROVIDER="disabled"
$env:FINBRIDGE_LLM_ENABLED="false"
mvn spring-boot:run
```

Unknown provider values stop application configuration with a clear error. Provider timeout, connection failure, HTTP error, invalid JSON or schema mismatch produces the existing safe unavailable message. Fallback never creates a chat-driven payment plan; the guided deterministic tuition workflow remains usable.

The provider response selects only supported backend-owned personal-finance or tuition intents.
Cheapest/fastest preferences apply only to existing tuition actions; read-only insights require NONE.
Amounts, recipients, accounts, bills, quotes, currencies, corridors, approval and policy outcomes are loaded and
validated by deterministic backend services. Missing configuration, provider errors and invalid structured output
fall back without creating or executing a payment plan.


## Read-only AI Personal Finance

Ask in the Overview chat about spending in the current demo month, configured category budgets,
or the projected balance after the selected verified tuition bill. Ollama only classifies the
question; existing backend services calculate all amounts and backend templates explain their evidence.

Examples: "Where did I spend the most this month?", "Ngân sách tháng này còn bao nhiêu?",
and "Nếu đóng học phí thì còn đủ tiền sinh hoạt không?".

The scope is the current demo profile/month. Other periods, custom account/category scopes and
model-generated financial parameters require clarification. Refunds remain separate from gross
expenses; internal transfers are excluded, and currencies are never summed together. Budgets are
configured in VND. Tuition affordability is a synthetic projection using the selected Sandbox
funding account and its eligible, unexpired quote. Planner commitments are shown with an explicit
conservative assumption because personal planner and payment accounts are separate sources.

The chat shows processing, blocks duplicate submissions, restores controls after errors/timeouts,
and discards responses superseded by reset or a newer request. No automatic retry is performed.
Responses to the three read-only intents change only session display/context and audit records in the web workflow.

Targeted checks:

    mvn "-Dtest=PersonalFinanceAiIntegrationTest,PersonalFinanceAiPlaywrightTest,FinBridgePlaywrightE2ETest,TransactionServiceTest" test

Separate live model and browser verification (requires installed qwen3:4b and running local Ollama;
starts its own synthetic FinBridge instance on port 8091):

    mvn "-Dtest=PersonalFinanceOllamaLiveIT" test

The default Maven test suite uses stubs for model responses and does not depend on Ollama.
See [the Vietnamese verification report](docs/AI_PERSONAL_FINANCE_VERIFICATION.md) for measured results and limitations.


## Demo preparation and full-story rehearsal

`scripts/start-local.ps1` includes a read-only application warmup. For an already running app, use
`./scripts/prepare-demo.ps1 -FinBridgeUrl http://localhost:8080`. The original
`./scripts/prepare-demo.ps1` without this argument still warms model weights only; also ask one
read-only budget question through FinBridge before presenting in that case.
Reset synthetic application data if needed and begin the demo promptly; warming model weights alone does not
guarantee the first classifier request is fast. Keep the existing 60-second timeout and fallback.

Opt-in real-Ollama browser rehearsal (synthetic database, port 8093):

    mvn "-Dtest=DemoRehearsalOllamaLiveIT" test

See [the Vietnamese demo readiness guide](docs/DEMO_READINESS.md) for the workflow, focused test evidence
and the transaction timestamp precision fix.

## Session-scoped contextual conversations (Task A)

The web assistant remembers a supported topic and backend-verified object references in the current
HTTP session. English/Vietnamese follow-ups include "How much is left?" / "Còn bao nhiêu?",
"What about the fastest option?" / "Còn kênh nhanh nhất?", and "What's its status?" / "Trạng thái thế nào?".
Ask about tuition and Explain this plan bind to server-issued, session-scoped opaque references.
Ambiguous questions offer explicit choices; selecting a choice never creates a plan or calls the model.

Only the current message plus enum-level topic/channel/clarification metadata goes to the model.
The same strict four-field output contract is used. Display history is bounded, session-private and
never sent as model input or treated as financial truth. Financial data is reloaded on every turn.
An explicit supported draft request can create only a plan through the existing guarded workflow;
payment approval remains on the payment screen. Living-expense runway is not implemented.

Tabs sharing the same browser session cookie share context/history. Separate browser profiles or
private contexts have separate conversational state, while the synthetic demo financial workspace
remains shared. Reset clears server context/history/choices across registered sessions. Bill/account/
plan changes or stale quote references invalidate context; the assistant does not refresh quotes silently.
Restarting FinBridge or expiring the HTTP session removes transient memory. No persistent or distributed
conversation memory is added.

Targeted backend and browser checks (mock model responses):

    mvn "-Dtest=SessionConversationIntegrationTest,ContextualConversationPlaywrightTest" test

Opt-in real Ollama multi-turn browser verification (synthetic database, port 8104):

    mvn "-Dtest=ContextualConversationOllamaLiveIT" test

Full existing regression gate:

    mvn test

See [the Vietnamese Task A verification report](docs/CONTEXTUAL_CONVERSATION_VERIFICATION.md)
for results, lifecycle, financial invariance and protected-code diff evidence.
