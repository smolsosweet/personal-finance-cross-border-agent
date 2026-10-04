# Deploy FinBridge — bản demo dùng chung

## Phạm vi

- Spring Boot Java 21, Docker, Gemini qua backend; không cần Ollama trên máy người xem.
- Một instance và một workspace tài chính/Sandbox dùng chung. Chat/context tách theo cookie phiên.
- Chỉ dữ liệu mô phỏng và Payment Sandbox; chưa có đăng nhập hoặc tài khoản riêng.
- Profile `hosting` dùng H2 trong bộ nhớ. Restart/redeploy/spin-down khởi tạo lại seed, mất các thay đổi, receipt và audit của lần chạy trước. Reload trang không reset database.
- Người xem có thể sửa dữ liệu/reset của cả team. URL công khai chưa có xác thực hoặc hạn mức API theo người dùng; chỉ dùng để nghiệm thu demo, không dùng dữ liệu thật.

## Render (khuyến nghị)

1. Sau gate kiểm chứng đạt, push commit đã test lên GitHub.
2. Render Dashboard → New → Blueprint → chọn repo `smolsosweet/personal-finance-cross-border-agent` và branch chứa commit đã test.
3. Render đọc `render.yaml`: Docker, Free, Singapore, một instance, không disk/database ngoài, health check `/`. Kiểm tra UI vẫn ghi Free trước khi tạo.
4. Điền `GEMINI_API_KEY` trực tiếp trong secrets/environment của Render; không gửi key vào chat, không commit key, không dùng build argument. Giữ model `gemini-3.5-flash-lite`.
5. Deploy, đối chiếu commit ID trong Events và chờ service Live. Không khai báo `SPRING_DATASOURCE_URL`, không override profile hosting.
6. Lấy URL HTTPS do Render cấp để smoke test. `autoDeployTrigger: off` giúp chỉ deploy bản đã test qua thao tác Manual Deploy.

Render Free có thể ngủ sau 15 phút không có lưu lượng; mở lại cần cold start khoảng một phút. Với profile này, lần khởi động đó trở về seed. Free không có persistent disk. Xem [Free](https://render.com/docs/free), [Docker](https://render.com/docs/docker), [Blueprint](https://render.com/docs/blueprint-spec).

## Kiểm chứng container local (không gọi model)

```bat
docker build -t finbridge-shared-demo:context-stability .
docker run -d --name finbridge-hosting-check -p 8125:8080 -e FINBRIDGE_LLM_ENABLED=false finbridge-shared-demo:context-stability
```

Mở `http://localhost:8125`. Kiểm tra notice tiếng Việt/Anh. Tạo giao dịch mô phỏng hoặc draft bằng luồng có hướng dẫn, rồi `docker restart finbridge-hosting-check`: dữ liệu phải trở về seed, không còn draft/receipt. Dừng container sau kiểm chứng bằng `docker stop finbridge-hosting-check`.

## Smoke URL thật

- Hai cửa sổ riêng: cùng thấy dữ liệu tài chính, nhưng không thấy tin nhắn/context của nhau.
- Ngân sách → hỏi vì sao Bank B không dùng được → hỏi “kênh đó” → runway: tài khoản/kênh của kịch bản hợp lệ phải được giữ. Xác nhận 8.000.000 VND/tháng qua form, số tháng do backend tính.
- So sánh kênh → yêu cầu tạo draft: chỉ `AWAITING_APPROVAL`; chưa duyệt thì không receipt hoặc thay đổi số dư.
- Không dùng Bank B để thực thi. Injection không thay người nhận/quyền/tiền. Emergency Stop phải chặn luồng có hướng dẫn.
- Refresh báo giá nếu quá 5 phút. Không dùng báo giá hết hạn làm bằng chứng.
- Chỉ một người điều khiển luồng tài chính lúc thuyết trình; Reset và restart ảnh hưởng cả team.
- Lỗi/quota Gemini có fallback rõ; không retry liên tục hoặc tạo draft qua fallback. Startup/health check không gửi model request.

## Ranh giới

Không có billing activation, real bank/payment integration, RAG hoặc AI phase mới. Giữ Policy Guard, phép tính tài chính, strict parser, confidence và approval riêng như trước. Chuẩn bị Docker/profile chưa đồng nghĩa URL cloud đã được deploy hoặc smoke test thành công; kết quả thực tế được ghi trong `GEMINI_VERIFICATION.md`.
