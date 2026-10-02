package com.example.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Model I/O stays outside database locks. Only current requests may publish or create a draft. */
@Service
public class FinanceChatService {
    private static final Pattern VI = Pattern.compile("[À-ỹ]");
    private static final Pattern OTHER_PERIOD = Pattern.compile(
            "(?iu)\\b(last|next|previous|past)\\s+(month|week|year)|\\b(yesterday|tomorrow|weekly|annual|january|february|march|april|june|july|august|september|october|november|december)\\b|tháng\\s+(trước|sau|tới|\\d+)|tuần|năm\\s+(trước|sau|nay)|hôm\\s+(qua|nay)|ngày\\s+mai|\\b20\\d{2}\\b|\\d{1,2}[/-]\\d{1,2}");
    private final JdbcTemplate db;
    private final LlmIntentClient llm;
    private final PersonalFinanceInsights insights;
    private final TransactionTemplate tx;

    public FinanceChatService(JdbcTemplate db, LlmIntentClient llm, PersonalFinanceInsights insights,
                              PlatformTransactionManager transactionManager) {
        this.db = db;
        this.llm = llm;
        this.insights = insights;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public String send(String raw, String language, PhaseFourService payments) {
        String message = raw == null ? "" : raw.trim();
        if (message.isBlank()) throw new IllegalArgumentException("Message is required");
        if (message.length() > 500) throw new IllegalArgumentException("Message must be 500 characters or fewer");
        boolean vi = "vi".equals(language) || (language == null && VI.matcher(message).find());
        String requestId = "CHAT-" + UUID.randomUUID().toString().substring(0, 12);
        boolean online = Boolean.TRUE.equals(tx.execute(status -> {
            lock();
            addMessage("USER", message);
            audit("USER", "CONVERSATION_INPUT", requestId, "RECEIVED", null, "Untrusted user message recorded");
            audit("LLM_INTENT", "AI_REQUEST_STARTED", requestId, "PENDING", null, "Classifier request; no financial payload logged");
            return llm.enabled() && !"OFFLINE".equals(payments.policy().runtimeMode());
        }));
        String lower = message.toLowerCase(Locale.ROOT);
        LlmIntent intent = null;
        String early = null;
        String outcome = "COMPLETED";
        String reason = null;
        if (PhaseFourService.isInjection(message)) {
            early = "This request cannot change payment safety controls or bypass approval.";
            outcome = "BLOCKED"; reason = "UNTRUSTED INSTRUCTION";
        } else if (OTHER_PERIOD.matcher(message).find()) {
            early = clarification(vi);
            reason = "UNSUPPORTED PERIOD";
        } else if (lower.contains("surplus") || ((lower.contains("balance") || lower.contains("số dư"))
                && !Pattern.compile("(?iu)tuition|university|fee|budget|spend|học phí|ngân sách|chi tiêu|sinh hoạt|after|remaining|project").matcher(message).find())) {
            early = payments.deterministicFallback(message);
            reason = "GUIDED BALANCE";
        } else if (!online) {
            early = payments.deterministicFallback(message);
            outcome = "FALLBACK"; reason = "LLM UNAVAILABLE";
        } else {
            try {
                intent = llm.classify(message);
            } catch (LlmIntentException ex) {
                early = vi
                        ? "AI tạm thời không khả dụng hoặc đã hết thời gian chờ. Chưa tạo kế hoạch hay thanh toán. Bạn có thể xem tổng quan hoặc dùng luồng học phí có hướng dẫn."
                        : "AI is temporarily unavailable or timed out. No plan or payment was created. View the overview or use the guided tuition flow.";
                outcome = "FALLBACK"; reason = "LLM UNAVAILABLE";
            }
        }
        LlmIntent classified = intent;
        String response = early, finalOutcome = outcome, finalReason = reason;
        return tx.execute(status -> {
            lock();
            if (!isCurrent(requestId)) {
                return vi ? "Yêu cầu đã được thay thế hoặc reset; kết quả cũ đã bỏ qua." : "Request superseded or reset; old result discarded.";
            }
            String answer;
            if (response != null) {
                answer = response;
                audit(finalReason != null && finalReason.equals("UNTRUSTED INSTRUCTION") ? "POLICY_GUARD" : "LLM_INTENT",
                        "UNTRUSTED INSTRUCTION".equals(finalReason) ? "INPUT_BLOCKED"
                                : "LLM UNAVAILABLE".equals(finalReason) ? "INTENT_PROVIDER_UNAVAILABLE" : "AI_REQUEST_CLARIFICATION",
                        requestId, finalOutcome, finalReason, "Backend-generated safe response; no financial action");
            } else {
                audit("LLM_INTENT", "INTENT_CLASSIFIED", requestId, "COMPLETED", classified.intent().name(),
                        "Preference " + classified.channelPreference() + "; confidence " + confidenceBand(classified.confidence()));
                if (isReadOnly(classified.intent())) {
                    if (classified.confidence().compareTo(new BigDecimal("0.80")) < 0
                            || scopedQuestion(message)) {
                        answer = clarification(vi);
                        audit("FINANCE_READ_ONLY", "READ_ONLY_CLARIFICATION", requestId, "CLARIFICATION",
                                "AMBIGUOUS_REQUEST", "Confidence or scope requires clarification");
                    } else {
                        answer = insights.render(classified.intent(), payments, vi);
                        audit("FINANCE_READ_ONLY", "READ_ONLY_RESULT", requestId, "COMPLETED", classified.intent().name(),
                                "Current demo data; deterministic aggregation; no financial mutation");
                    }
                } else if (classified.intent() == LlmIntent.Intent.NEED_CLARIFICATION) {
                    answer = clarification(vi);
                } else {
                    answer = payments.renderIntent(classified);
                }
            }
            addMessage("ASSISTANT", answer);
            audit("LLM_INTENT", "AI_REQUEST_FINISHED", requestId, finalOutcome,
                    classified == null ? finalReason : classified.intent().name(), "Request finished; response comes from backend templates");
            return answer;
        });
    }

    static boolean isReadOnly(LlmIntent.Intent intent) {
        return intent == LlmIntent.Intent.EXPLAIN_SPENDING_SUMMARY
                || intent == LlmIntent.Intent.EXPLAIN_BUDGET_STATUS
                || intent == LlmIntent.Intent.EXPLAIN_TUITION_AFFORDABILITY;
    }

    private static boolean scopedQuestion(String message) {
        return Pattern.compile("(?iu)\\b(another|someone|custom|specific|both|account|cash|food|transport|groceries|housing|shopping|Bank A|Bank B|Alipay|MoMo|CNY|USD|AUD)\\b|tài khoản|tiền mặt|của (bạn|người)|riêng|đồng thời|ăn uống tháng|đi lại tháng|chi.*ăn uống|\\d")
                .matcher(message).find();
    }

    static String clarification(boolean vi) {
        return vi
                ? "Hãy chọn một câu hỏi cho tháng demo hiện tại và hồ sơ hiện có: tổng chi tiêu theo danh mục, ngân sách còn lại, hoặc dự kiến số tiền sau học phí. Chưa hỗ trợ kỳ khác hay phạm vi tùy chọn."
                : "Please clarify whether you want spending, budgets, or tuition affordability for the current demo month and existing profile: spending by category, remaining budgets, or projected balance after tuition. Other periods or custom scopes are not supported.";
    }

    private static String confidenceBand(BigDecimal value) {
        return value.compareTo(new BigDecimal("0.80")) >= 0 ? "HIGH"
                : value.compareTo(new BigDecimal("0.50")) >= 0 ? "MEDIUM" : "LOW";
    }

    private void lock() { db.queryForObject("SELECT id FROM agent_policy WHERE id=1 FOR UPDATE", Integer.class); }

    private boolean isCurrent(String id) {
        var rows = db.queryForList("""
                SELECT reference_id FROM audit_log WHERE event_type='AI_REQUEST_STARTED'
                ORDER BY occurred_at DESC,id DESC FETCH FIRST 1 ROW ONLY
                """, String.class);
        return !rows.isEmpty() && id.equals(rows.getFirst());
    }

    private void addMessage(String role, String text) {
        // Existing schema caps each message at 1000 characters; preserve complete evidence over multiple messages.
        for (int offset = 0; offset < text.length();) {
            int end = Math.min(offset + 990, text.length());
            if (end < text.length()) {
                int newline = text.lastIndexOf('\n', end);
                if (newline > offset) end = newline + 1;
            }
            db.update("INSERT INTO conversation_messages VALUES (?,?,?,?)",
                    "MSG-" + UUID.randomUUID().toString().substring(0, 12), role,
                    text.substring(offset, end), nextTimestamp("conversation_messages", "created_at"));
            offset = end;
        }
    }

    private LocalDateTime nextTimestamp(String table, String column) {
        // All callers hold the existing workflow lock. Preserve request/chunk order at DB microsecond precision.
        LocalDateTime latest = db.queryForObject("SELECT MAX(" + column + ") FROM " + table, LocalDateTime.class);
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        return latest != null && !now.isAfter(latest) ? latest.plusNanos(1000) : now;
    }

    private void audit(String actor, String event, String ref, String status, String reason, String details) {
        db.update("INSERT INTO audit_log VALUES (?,?,?,?,?,?,?,?)",
                "AUD-" + UUID.randomUUID().toString().substring(0, 12), nextTimestamp("audit_log", "occurred_at"),
                actor, event, ref, status, reason, details);
    }
}
