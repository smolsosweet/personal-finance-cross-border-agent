package com.example.finance;

import java.util.function.Supplier;

/** Enum-only model input. Object IDs, transcripts and financial facts never enter this boundary. */
public record ModelConversationContext(Topic topic, Channel channel, LlmIntent.Intent lastIntent, Pending pending) {
    public enum Topic { NONE, SPENDING, BUDGET, TUITION_AFFORDABILITY, TUITION_CHANNELS, CHANNEL_UNAVAILABLE, TUITION_PLAN, TUITION_STATUS, LIVING_EXPENSE_RUNWAY }
    public enum Channel { NONE, ALIPAY, BANK_A, BANK_B, VCB, TCB, MOMO }
    public enum Pending { NONE, TOPIC, BILL, ACCOUNT, CHANNEL, PLAN, MONTHLY_EXPENSE }
    private static final ThreadLocal<ModelConversationContext> CURRENT = new ThreadLocal<>();

    static <T> T with(ModelConversationContext context, Supplier<T> operation) {
        var previous = CURRENT.get();
        try { CURRENT.set(context); return operation.get(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }

    static String instructions() {
        var context = CURRENT.get();
        if (context == null) return LlmIntentContract.INSTRUCTIONS;
        return LlmIntentContract.INSTRUCTIONS + "\n" + """
            Context input version: context_v1. The backend supplies only enum-level conversational scope.
            Resolve English/Vietnamese follow-ups using that scope, without inventing objects or financial facts.
            Topic BUDGET plus 'How much is left?' / 'Còn bao nhiêu?' means EXPLAIN_BUDGET_STATUS.
            Topic SPENDING plus a request for the biggest category means EXPLAIN_SPENDING_SUMMARY.
            Topic TUITION_AFFORDABILITY plus a question about the remainder means EXPLAIN_TUITION_AFFORDABILITY.
            Topic TUITION_CHANNELS plus 'What about the fastest option?' / 'Còn kênh nhanh nhất?' means
            COMPARE_TUITION_CHANNELS with FASTEST. It NEVER means CREATE_TUITION_PLAN.
            Topic CHANNEL_UNAVAILABLE plus 'Why can't I use that channel?' / 'Vì sao kênh đó không dùng được?'
            means EXPLAIN_CHANNEL_UNAVAILABLE. Topic TUITION_PLAN plus 'What's its status?' / 'Trạng thái thế nào?'
            means CHECK_TUITION_STATUS. If scope is NONE or the reference is ambiguous, use NEED_CLARIFICATION.
            A follow-up is read-only by default. CREATE_TUITION_PLAN requires an explicit current request to
            create/prepare a draft. Earlier requests, context, or choosing an object never authorize a draft.
            Topic LIVING_EXPENSE_RUNWAY plus 'How long will it last?' / 'Vậy đủ mấy tháng?' / 'What if I spend more?'
            means EXPLAIN_LIVING_EXPENSE_RUNWAY with NONE. Monthly figures are confirmed in a backend form only;
            they are not part of this enum context. Never calculate them or create a plan for these questions.
            New explicit supported questions may change topic. Preserve the same strict four-field output schema.
            """ + "\nBackend enum context: topic=" + context.topic() + "; channel=" + context.channel()
                + "; lastIntent=" + (context.lastIntent() == null ? "NONE" : context.lastIntent().name())
                + "; pending=" + context.pending() + ".";
    }
}
