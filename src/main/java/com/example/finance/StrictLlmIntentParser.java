package com.example.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class StrictLlmIntentParser {
    private static final Set<String> FIELDS = Set.of(
            "intent", "channelPreference", "confidence", "clarificationCode");

    private final ObjectMapper mapper;

    public StrictLlmIntentParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public LlmIntent parse(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            if (root == null || !root.isObject() || root.size() != FIELDS.size()) {
                throw invalid();
            }
            root.fieldNames().forEachRemaining(name -> {
                if (!FIELDS.contains(name)) throw invalid();
            });
            for (String field : FIELDS) {
                if (!root.hasNonNull(field)) throw invalid();
            }
            if (!root.path("intent").isTextual()
                    || !root.path("channelPreference").isTextual()
                    || !root.path("confidence").isNumber()
                    || !root.path("clarificationCode").isTextual()) {
                throw invalid();
            }

            LlmIntent.Intent intent = LlmIntent.Intent.valueOf(root.path("intent").textValue());
            LlmIntent.ChannelPreference preference = LlmIntent.ChannelPreference.valueOf(
                    root.path("channelPreference").textValue());
            LlmIntent.ClarificationCode clarification = LlmIntent.ClarificationCode.valueOf(
                    root.path("clarificationCode").textValue());
            BigDecimal confidence = root.path("confidence").decimalValue();
            if (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0) {
                throw invalid();
            }
            if (intent != LlmIntent.Intent.NEED_CLARIFICATION
                    && clarification != LlmIntent.ClarificationCode.NONE) {
                throw invalid();
            }
            if (intent != LlmIntent.Intent.CREATE_TUITION_PLAN
                    && intent != LlmIntent.Intent.COMPARE_TUITION_CHANNELS
                    && preference != LlmIntent.ChannelPreference.NONE) {
                throw invalid();
            }
            if (intent.name().startsWith("EXPLAIN_")
                    && intent != LlmIntent.Intent.EXPLAIN_CHANNEL_UNAVAILABLE
                    && preference != LlmIntent.ChannelPreference.NONE) throw invalid();
            return new LlmIntent(intent, preference, confidence, clarification);
        } catch (LlmIntentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LlmIntentException("LLM response did not match the FinBridge intent contract", ex);
        }
    }

    private static LlmIntentException invalid() {
        return new LlmIntentException("LLM response did not match the FinBridge intent contract");
    }
}
