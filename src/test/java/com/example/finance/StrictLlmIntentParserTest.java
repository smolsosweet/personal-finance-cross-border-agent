package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StrictLlmIntentParserTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final StrictLlmIntentParser parser = new StrictLlmIntentParser(mapper);

    @Test void balanceIntentsUseTheStrictGeminiSchema() {
        for (String intent : java.util.List.of("EXPLAIN_CURRENT_BALANCE", "EXPLAIN_RECEIPT_BALANCE")) {
            String json = "{\"intent\":\"" + intent + "\",\"channelPreference\":\"NONE\",\"confidence\":0.95,\"clarificationCode\":\"NONE\"}";
            assertEquals(intent, parser.parse(json).intent().name());
            assertThrows(LlmIntentException.class, () -> parser.parse(json.replace("\"NONE\",\"confidence\"", "\"CHEAPEST\",\"confidence\"")));
            assertThrows(LlmIntentException.class, () -> parser.parse(json.replace("}", ",\"accountId\":\"PAYER_VND\"}")));
            assertThrows(LlmIntentException.class, () -> parser.parse(json.replace("}", ",\"balance\":100000000}")));
        }
        var schema = LlmIntentContract.schema();
        assertEquals(false, schema.get("additionalProperties"));
        var gemini = new GeminiIntentClient(mapper, parser, true, "synthetic-key", "gemini-3.5-flash-lite",
                Duration.ofSeconds(2), Duration.ofSeconds(2));
        Map<String, Object> request = gemini.requestBody("Current balance");
        assertTrue(request.toString().contains("EXPLAIN_CURRENT_BALANCE"));
        assertTrue(request.toString().contains("EXPLAIN_RECEIPT_BALANCE"));
    }

    @Test void acceptsOnlyTheBackendOwnedIntentContract() {
        LlmIntent result = parser.parse("""
                {"intent":"CREATE_TUITION_PLAN","channelPreference":"CHEAPEST",
                 "confidence":0.94,"clarificationCode":"NONE"}
                """);
        assertEquals(LlmIntent.Intent.CREATE_TUITION_PLAN, result.intent());
        assertEquals(LlmIntent.ChannelPreference.CHEAPEST, result.channelPreference());
        assertEquals(0, result.confidence().compareTo(new java.math.BigDecimal("0.94")));
    }

    @Test void rejectsInvalidJsonUnsupportedIntentAndAdditionalFinancialFields() {
        assertThrows(LlmIntentException.class, () -> parser.parse("not-json"));
        assertThrows(LlmIntentException.class, () -> parser.parse("""
                {"intent":"TRANSFER_NOW","channelPreference":"NONE",
                 "confidence":0.8,"clarificationCode":"NONE"}
                """));
        assertThrows(LlmIntentException.class, () -> parser.parse("""
                {"intent":"CREATE_TUITION_PLAN","channelPreference":"CHEAPEST",
                 "confidence":0.9,"clarificationCode":"NONE","amount":20000}
                """));
    }
}
