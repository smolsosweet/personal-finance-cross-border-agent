package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StrictLlmIntentParserTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final StrictLlmIntentParser parser = new StrictLlmIntentParser(mapper);
    @Test void balanceIntentsUseSameStrictSchemaAcrossAllProviders(){
        for(String intent:java.util.List.of("EXPLAIN_CURRENT_BALANCE","EXPLAIN_RECEIPT_BALANCE")){
            String json="{\"intent\":\""+intent+"\",\"channelPreference\":\"NONE\",\"confidence\":0.95,\"clarificationCode\":\"NONE\"}";
            assertEquals(intent,parser.parse(json).intent().name());
            assertThrows(LlmIntentException.class,()->parser.parse(json.replace("\"NONE\",\"confidence\"","\"CHEAPEST\",\"confidence\"")));
            assertThrows(LlmIntentException.class,()->parser.parse(json.replace("}",",\"accountId\":\"PAYER_VND\"}")));
            assertThrows(LlmIntentException.class,()->parser.parse(json.replace("}",",\"balance\":100000000}")));
        }
        var schema=LlmIntentContract.schema();assertEquals(false,schema.get("additionalProperties"));
        for(Object provider:java.util.List.of(
                new OpenAiIntentClient(mapper,parser,true,"synthetic","test-model").requestBody("Current balance"),
                new OllamaIntentClient(mapper,parser,true,"http://localhost:11434","qwen3:4b",java.time.Duration.ofSeconds(2),java.time.Duration.ofSeconds(2)).requestBody("Current balance"))){
            String request=provider.toString();assertTrue(request.contains("EXPLAIN_CURRENT_BALANCE"));assertTrue(request.contains("EXPLAIN_RECEIPT_BALANCE"));
        }
    }

    @Test
    void acceptsOnlyTheBackendOwnedIntentContract() {
        LlmIntent result = parser.parse("""
                {"intent":"CREATE_TUITION_PLAN","channelPreference":"CHEAPEST",
                 "confidence":0.94,"clarificationCode":"NONE"}
                """);

        assertEquals(LlmIntent.Intent.CREATE_TUITION_PLAN, result.intent());
        assertEquals(LlmIntent.ChannelPreference.CHEAPEST, result.channelPreference());
        assertEquals(0, result.confidence().compareTo(new java.math.BigDecimal("0.94")));
    }

    @Test
    void rejectsInvalidJsonUnsupportedIntentAndAdditionalFinancialFields() {
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

    @Test
    void openAiRequestUsesStrictSchemaAndMissingConfigurationFailsClosed() {
        OpenAiIntentClient client = new OpenAiIntentClient(mapper, parser, true, "", "test-model");

        assertFalse(client.enabled());
        assertThrows(LlmIntentException.class, () -> client.classify("Prepare my tuition payment"));

        OpenAiIntentClient configured = new OpenAiIntentClient(
                mapper, parser, true, "test-key-never-sent", "test-model");
        Map<String,Object> request = configured.requestBody("Compare tuition options");
        assertEquals(false, request.get("store"));
        assertFalse(request.containsKey("tools"));
        @SuppressWarnings("unchecked")
        Map<String,Object> text = (Map<String,Object>) request.get("text");
        @SuppressWarnings("unchecked")
        Map<String,Object> format = (Map<String,Object>) text.get("format");
        @SuppressWarnings("unchecked")
        Map<String,Object> schema = (Map<String,Object>) format.get("schema");
        assertEquals(true, format.get("strict"));
        assertEquals(false, schema.get("additionalProperties"));
        @SuppressWarnings("unchecked")
        Map<String,Object> properties = (Map<String,Object>) schema.get("properties");
        assertEquals(java.util.Set.of("intent", "channelPreference", "confidence", "clarificationCode"),
                properties.keySet());
    }

    @Test
    void extractsOnlyCompletedOutputTextAndRejectsRefusalOrIncompleteResponses() throws Exception {
        OpenAiIntentClient client = new OpenAiIntentClient(
                mapper, parser, true, "test-key-never-sent", "test-model");
        String output = client.extractOutputText(mapper.readTree("""
                {"status":"completed","output":[{"type":"message","content":[
                  {"type":"output_text","text":"structured-result"}
                ]}]}
                """));
        assertEquals("structured-result", output);
        assertThrows(LlmIntentException.class, () -> client.extractOutputText(mapper.readTree("""
                {"status":"completed","output":[{"type":"message","content":[
                  {"type":"refusal","refusal":"Cannot comply"}
                ]}]}
                """)));
        assertThrows(LlmIntentException.class,
                () -> client.extractOutputText(mapper.readTree("{" + "\"status\":\"incomplete\"}")));
    }
}
