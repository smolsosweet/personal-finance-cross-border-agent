package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GeminiIntentClientTest {
    static final ObjectMapper MAPPER = new ObjectMapper();
    static final String VALID = "{\"intent\":\"EXPLAIN_BUDGET_STATUS\",\"channelPreference\":\"NONE\",\"confidence\":0.95,\"clarificationCode\":\"NONE\"}";

    @Test void headerSchemaAndEnumOnlySessionContextUseSharedContract() throws Exception {
        try (var stub = new Stub()) {
            var client=client(stub, Duration.ofSeconds(2),true);
            var context=new ModelConversationContext(ModelConversationContext.Topic.BUDGET,ModelConversationContext.Channel.NONE,
                    LlmIntent.Intent.EXPLAIN_BUDGET_STATUS,ModelConversationContext.Pending.NONE);
            var result=ModelConversationContext.with(context,()->client.classify("Còn bao nhiêu?"));
            assertEquals(LlmIntent.Intent.EXPLAIN_BUDGET_STATUS,result.intent());
            assertEquals("synthetic-test-key",stub.key);assertNull(stub.query);
            assertEquals("POST",stub.method);assertEquals("application/json",stub.contentType);
            var body=MAPPER.readTree(stub.body);
            assertEquals(3,body.size());assertFalse(body.has("tools"));
            assertEquals(MAPPER.valueToTree(LlmIntentContract.schema()),body.path("generationConfig").path("responseJsonSchema"));
            assertFalse(body.path("generationConfig").path("responseJsonSchema").path("additionalProperties").asBoolean(true));
            assertEquals("application/json",body.path("generationConfig").path("responseMimeType").asText());
            assertEquals("Còn bao nhiêu?",body.path("contents").get(0).path("parts").get(0).path("text").asText());
            String system=body.path("systemInstruction").path("parts").get(0).path("text").asText();
            assertTrue(system.contains("topic=BUDGET"));assertTrue(system.startsWith(LlmIntentContract.INSTRUCTIONS));
            assertFalse(stub.body.contains("SZDU-TUITION"));assertFalse(stub.body.contains("synthetic-test-key"));
            assertFalse(client.requestBody("fresh").toString().contains("topic=BUDGET"));
            assertEquals(1,stub.calls.get());
        }
    }

    @ParameterizedTest @ValueSource(strings={
        "not-json", "```json\n{}\n```", "{}", "[]",
        "{\"intent\":\"TRANSFER_NOW\",\"channelPreference\":\"NONE\",\"confidence\":0.9,\"clarificationCode\":\"NONE\"}",
        "{\"intent\":\"EXPLAIN_BUDGET_STATUS\",\"channelPreference\":\"NONE\",\"confidence\":\"0.9\",\"clarificationCode\":\"NONE\"}",
        "{\"intent\":\"EXPLAIN_BUDGET_STATUS\",\"channelPreference\":\"FASTEST\",\"confidence\":0.9,\"clarificationCode\":\"NONE\"}",
        "{\"intent\":\"EXPLAIN_BUDGET_STATUS\",\"channelPreference\":\"NONE\",\"confidence\":1.1,\"clarificationCode\":\"NONE\"}",
        "{\"intent\":\"EXPLAIN_BUDGET_STATUS\",\"channelPreference\":\"NONE\",\"confidence\":0.9,\"clarificationCode\":\"NONE\",\"amount\":20000}"
    }) void invalidContractOutputIsNeverRepaired(String output) throws Exception {
        try(var stub=new Stub()) {stub.response=envelope(output,"STOP");assertFailure(stub,"GEMINI_INVALID_OUTPUT");}
    }

    @ParameterizedTest @ValueSource(strings={"null","[]","not-json","{}","{\"candidates\":[]}",
        "{\"candidates\":[{},{}]}","{\"candidates\":[{\"finishReason\":\"STOP\"}]}",
        "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"role\":\"model\",\"parts\":[{\"functionCall\":{}}]}}]}",
        "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"role\":\"user\",\"parts\":[{\"text\":\"{}\"}]}}]}"
    }) void malformedOrUnexpectedEnvelopeFailsClosed(String response) throws Exception {
        try(var stub=new Stub()) {stub.response=response;assertFailure(stub,"GEMINI_INVALID_OUTPUT");}
    }

    @ParameterizedTest @CsvSource({"SAFETY,GEMINI_OUTPUT_BLOCKED","RECITATION,GEMINI_OUTPUT_BLOCKED",
        "PROHIBITED_CONTENT,GEMINI_OUTPUT_BLOCKED","MAX_TOKENS,GEMINI_OUTPUT_TRUNCATED","OTHER,GEMINI_INVALID_OUTPUT",
        "FINISH_REASON_UNSPECIFIED,GEMINI_INVALID_OUTPUT"})
    void refusedAndTruncatedResponsesDiscardEvenValidJson(String finish,String code) throws Exception {
        try(var stub=new Stub()) {stub.response=envelope(VALID,finish);assertFailure(stub,code);}
    }

    @Test void blockedPromptAndBlockedCandidateAreDiscarded() throws Exception {
        try(var stub=new Stub()) {
            stub.response="{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";assertFailure(stub,"GEMINI_OUTPUT_BLOCKED");
            var root=MAPPER.readTree(envelope(VALID,"STOP"));
            ((com.fasterxml.jackson.databind.node.ObjectNode)root.path("candidates").get(0)).putArray("safetyRatings").addObject().put("blocked",true);
            stub.response=MAPPER.writeValueAsString(root);assertFailure(stub,"GEMINI_OUTPUT_BLOCKED");
        }
    }

    @ParameterizedTest @CsvSource({"401,GEMINI_AUTHENTICATION","403,GEMINI_AUTHENTICATION","404,GEMINI_MODEL_UNAVAILABLE",
        "429,GEMINI_QUOTA_EXHAUSTED","500,GEMINI_SERVICE_UNAVAILABLE","503,GEMINI_SERVICE_UNAVAILABLE","400,GEMINI_REQUEST_REJECTED",
        "302,GEMINI_REQUEST_REJECTED"})
    void statusCodesAreSanitizedWithoutRetryOrRedirect(int status,String code) throws Exception {
        try(var stub=new Stub()) {stub.status=status;stub.response="synthetic-sensitive-provider-body";
            assertFailure(stub,code);assertEquals(1,stub.calls.get());}
    }

    @Test void timeoutAndUnavailableConnectionUseSafeCodes() throws Exception {
        try(var stub=new Stub()) {stub.delay=250;
            var ex=assertThrows(GeminiProviderException.class,()->client(stub,Duration.ofMillis(50),true).classify("budget"));
            assertEquals("GEMINI_TIMEOUT",ex.reasonCode());assertNull(ex.getCause());}
        try(var stub=new Stub()) {
            var client=client(stub,Duration.ofMillis(300),true);stub.close();
            assertEquals("GEMINI_SERVICE_UNAVAILABLE",assertThrows(GeminiProviderException.class,()->client.classify("budget")).reasonCode());
        }
    }

    @ParameterizedTest @ValueSource(strings={"CONNECT_TIMEOUT","TIMEOUT"})
    void transportTimeoutStageIsSanitizedWithoutRetry(String stage) throws Exception {
        var transport=mock(HttpClient.class);
        var builder=mock(HttpClient.Builder.class,RETURNS_SELF);
        when(builder.build()).thenReturn(transport);
        var failure="CONNECT_TIMEOUT".equals(stage)
                ? new HttpConnectTimeoutException("synthetic-sensitive-key-and-url")
                : new HttpTimeoutException("synthetic-sensitive-key-and-url");
        when(transport.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(failure);
        try(var factory=mockStatic(HttpClient.class);var stub=new Stub()) {
            factory.when(HttpClient::newBuilder).thenReturn(builder);
            var ex=assertThrows(GeminiProviderException.class,()->client(stub,Duration.ofSeconds(2),true).classify("budget"));
            assertEquals("GEMINI_"+stage,ex.reasonCode());assertNull(ex.getCause());
            assertFalse(ex.getMessage().contains("synthetic-sensitive"));
            assertFalse(ex.help(true).contains("synthetic-sensitive"));assertFalse(ex.help(false).contains("synthetic-sensitive"));
            assertTrue(ex.help(true).contains("CONNECT_TIMEOUT".equals(stage)?"chưa kết nối":"chưa trả lời"));
            verify(transport,times(1)).send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
            assertEquals(0,stub.calls.get());
        }
    }

    @Test void disabledMakesZeroCallsAndInvalidConfigurationNeverContainsSecrets() throws Exception {
        try(var stub=new Stub()) {var disabled=client(stub,Duration.ofSeconds(1),false);
            assertFalse(disabled.enabled());assertThrows(GeminiProviderException.class,()->disabled.classify("budget"));assertEquals(0,stub.calls.get());}
        assertThrows(IllegalStateException.class,()->new GeminiIntentClient(MAPPER,new StrictLlmIntentParser(MAPPER),true,"","gemini-3.5-flash-lite",Duration.ofSeconds(1),Duration.ofSeconds(1)));
        var ex=assertThrows(IllegalStateException.class,()->new GeminiIntentClient(MAPPER,new StrictLlmIntentParser(MAPPER),true,"synthetic-secret","bad/model?key=synthetic-secret",Duration.ofSeconds(1),Duration.ofSeconds(1)));
        assertFalse(ex.getMessage().contains("synthetic-secret"));
        assertThrows(IllegalArgumentException.class,()->new GeminiIntentClient(MAPPER,new StrictLlmIntentParser(MAPPER),true,"synthetic-secret","gemini-3.5-flash-lite",Duration.ZERO,Duration.ofSeconds(1)));
    }

    static GeminiIntentClient client(Stub stub,Duration timeout,boolean enabled) {
        return new GeminiIntentClient(MAPPER,new StrictLlmIntentParser(MAPPER),enabled,"synthetic-test-key","gemini-3.5-flash-lite",Duration.ofSeconds(1),timeout,stub.endpoint());
    }
    static String envelope(String output,String finish) throws Exception {
        return MAPPER.writeValueAsString(Map.of("candidates",List.of(Map.of("finishReason",finish,
                "content",Map.of("role","model","parts",List.of(Map.of("text",output)))))));
    }
    private void assertFailure(Stub stub,String code) {
        var ex=assertThrows(GeminiProviderException.class,()->client(stub,Duration.ofSeconds(2),true).classify("budget"));
        assertEquals(code,ex.reasonCode());assertNull(ex.getCause());
        assertFalse(ex.getMessage().contains(stub.response));assertFalse(ex.getMessage().contains("synthetic-test-key"));
    }

    static final class Stub implements AutoCloseable {
        final HttpServer server;
        final AtomicInteger calls=new AtomicInteger();
        volatile String response,body,key,query,method,contentType;
        volatile int status=200;
        volatile long delay;
        Stub() throws Exception {
            response=envelope(VALID,"STOP");
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/generateContent",exchange->{
                calls.incrementAndGet();key=exchange.getRequestHeaders().getFirst("x-goog-api-key");query=exchange.getRequestURI().getRawQuery();
                method=exchange.getRequestMethod();contentType=exchange.getRequestHeaders().getFirst("Content-Type");
                body=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
                if(delay>0)try{Thread.sleep(delay);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
                byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
                try {exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);}
                catch(java.io.IOException ignored) {} finally {exchange.close();}
            });server.start();
        }
        URI endpoint(){return URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/generateContent");}
        public void close(){server.stop(0);}
    }
}
