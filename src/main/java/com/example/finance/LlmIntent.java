package com.example.finance;

import java.math.BigDecimal;

public record LlmIntent(Intent intent, ChannelPreference channelPreference,
                        BigDecimal confidence, ClarificationCode clarificationCode) {
    public enum Intent {
        CREATE_TUITION_PLAN,
        COMPARE_TUITION_CHANNELS,
        EXPLAIN_CHANNEL_UNAVAILABLE,
        CHECK_TUITION_STATUS,
        EXPLAIN_SPENDING_SUMMARY,
        EXPLAIN_BUDGET_STATUS,
        EXPLAIN_TUITION_AFFORDABILITY,
        EXPLAIN_LIVING_EXPENSE_RUNWAY,
        NEED_CLARIFICATION,
        UNSAFE_REQUEST,
        UNSUPPORTED_REQUEST
    }

    public enum ChannelPreference {
        CHEAPEST,
        FASTEST,
        NONE
    }

    public enum ClarificationCode {
        NONE,
        AMBIGUOUS_REQUEST,
        MISSING_TUITION_CONTEXT
    }
}
