package com.example.finance;

/** Only fixed messages/codes escape this adapter; provider bodies and causes are never retained. */
public final class GeminiProviderException extends LlmIntentException {
    public enum Reason {
        AUTHENTICATION, MODEL_UNAVAILABLE, QUOTA_EXHAUSTED, CONNECT_TIMEOUT, TIMEOUT, SERVICE_UNAVAILABLE,
        OUTPUT_BLOCKED, OUTPUT_TRUNCATED, INVALID_OUTPUT, REQUEST_REJECTED, DISABLED
    }
    private final Reason reason;

    GeminiProviderException(Reason reason) {
        super("Gemini intent provider: " + reason.name());
        this.reason = reason;
    }

    public String reasonCode() { return "GEMINI_" + reason.name(); }

    public String help(boolean vi) {
        return switch (reason) {
            case AUTHENTICATION -> vi ? "Cấu hình quyền truy cập Gemini không hợp lệ; hãy kiểm tra key và quyền API ở server."
                    : "Gemini authentication failed; check the server key and API permissions.";
            case MODEL_UNAVAILABLE -> vi ? "Model Gemini đã cấu hình không khả dụng với key này; hãy kiểm tra quyền truy cập model."
                    : "The configured Gemini model is unavailable to this key; check model access.";
            case QUOTA_EXHAUSTED -> vi ? "Gemini đã hết quota hoặc bị giới hạn tốc độ. Hãy thử lại sau; hệ thống không tự thử lại hay đổi model."
                    : "Gemini quota or rate limit reached. Try later; no automatic retry or model switch occurs.";
            case CONNECT_TIMEOUT -> vi ? "Server chưa kết nối được tới Gemini trong thời gian cho phép. Hãy thử lại sau."
                    : "The server could not connect to Gemini within the allowed time. Try again later.";
            case TIMEOUT -> vi ? "Gemini chưa trả lời trong thời gian cho phép. Hãy thử lại sau." : "Gemini did not respond within the allowed time. Try again later.";
            case OUTPUT_BLOCKED -> vi ? "Gemini từ chối hoặc chặn nội dung yêu cầu." : "Gemini refused or blocked this request.";
            case OUTPUT_TRUNCATED, INVALID_OUTPUT -> vi ? "Kết quả Gemini không đầy đủ hoặc sai cấu trúc; hệ thống đã bỏ qua."
                    : "Gemini output was incomplete or invalid and was discarded.";
            case REQUEST_REJECTED -> vi ? "Gemini từ chối cấu hình yêu cầu; hãy kiểm tra cấu hình ở server."
                    : "Gemini rejected the request configuration; check server configuration.";
            case SERVICE_UNAVAILABLE, DISABLED -> vi ? "Gemini hiện không khả dụng." : "Gemini is currently unavailable.";
        };
    }
}
