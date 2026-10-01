package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OllamaIntentClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final StrictLlmIntentParser parser = new StrictLlmIntentParser(mapper);

    @Test
    void actualOllamaEnvelopeProducesStrictIntentAndRequestUsesSchemaWithoutThinking() throws Exception {
        String content = """
                {"intent":"COMPARE_TUITION_CHANNELS","channelPreference":"FASTEST",
                 "confidence":0.91,"clarificationCode":"NONE"}
                """;
        try (StubOllama stub = new StubOllama(200, envelope(content, "ignore me"), 0)) {
            OllamaIntentClient client = client(stub.baseUrl(), Duration.ofSeconds(2));

            LlmIntent intent = client.classify("So sánh các kênh học phí nhanh nhất");

            assertEquals(LlmIntent.Intent.COMPARE_TUITION_CHANNELS, intent.intent());
            assertEquals(LlmIntent.ChannelPreference.FASTEST, intent.channelPreference());
            assertEquals(1, stub.calls.get());
            JsonNode request = mapper.readTree(stub.lastBody.get());
            assertEquals("qwen3:4b", request.path("model").asText());
            assertFalse(request.path("stream").asBoolean());
            assertFalse(request.path("think").asBoolean(true));
            assertEquals(0, request.path("options").path("temperature").asInt(-1));
            assertFalse(request.path("format").path("additionalProperties").asBoolean(true));
            assertEquals(4, request.path("format").path("properties").size());
            assertEquals("So sánh các kênh học phí nhanh nhất",
                    request.path("messages").get(1).path("content").asText());
            assertFalse(stub.lastBody.get().contains("SZDU-TUITION-2026"));
        }
    }

    @Test
    void clarificationAndConfidenceUseTheExistingStrictContract() throws Exception {
        String content = """
                {"intent":"NEED_CLARIFICATION","channelPreference":"NONE",
                 "confidence":0.42,"clarificationCode":"AMBIGUOUS_REQUEST"}
                """;
        try (StubOllama stub = new StubOllama(200, envelope(content, null), 0)) {
            LlmIntent result = client(stub.baseUrl(), Duration.ofSeconds(2)).classify("Giúp tôi việc đó");
            assertEquals(LlmIntent.Intent.NEED_CLARIFICATION, result.intent());
            assertEquals(0, result.confidence().compareTo(new java.math.BigDecimal("0.42")));
            assertEquals(LlmIntent.ClarificationCode.AMBIGUOUS_REQUEST, result.clarificationCode());
        }
    }

    @Test
    void malformedAndNonContractContentFailClosed() throws Exception {
        assertRejected("not-json");
        assertRejected("""
                {"intent":"CREATE_TUITION_PLAN","channelPreference":"CHEAPEST",
                 "confidence":0.9,"clarificationCode":"NONE","amount":20000}
                """);
        assertRejected("""
                {"intent":"TRANSFER_NOW","channelPreference":"NONE",
                 "confidence":0.9,"clarificationCode":"NONE"}
                """);
        assertRejected("""
                {"intent":"CHECK_TUITION_STATUS","channelPreference":"NONE",
                 "confidence":"high","clarificationCode":"NONE"}
                """);
        assertRejected("""
                {"intent":"CHECK_TUITION_STATUS","channelPreference":"QUICKEST",
                 "confidence":0.8,"clarificationCode":"NONE"}
                """);
    }

    @Test
    void providerHttpErrorTimeoutAndConnectionFailureFailClosed() throws Exception {
        try (StubOllama stub = new StubOllama(503, "unavailable", 0)) {
            assertThrows(LlmIntentException.class,
                    () -> client(stub.baseUrl(), Duration.ofSeconds(2)).classify("Check tuition"));
        }
        try (StubOllama stub = new StubOllama(200, envelope(validStatus(), null), 250)) {
            assertThrows(LlmIntentException.class,
                    () -> client(stub.baseUrl(), Duration.ofMillis(50)).classify("Check tuition"));
        }

        HttpServer stopped = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int unusedPort = stopped.getAddress().getPort();
        stopped.stop(0);
        assertThrows(LlmIntentException.class,
                () -> client("http://127.0.0.1:" + unusedPort, Duration.ofMillis(200))
                        .classify("Check tuition"));
    }

    @Test
    void disabledClientMakesNoProviderCall() throws Exception {
        try (StubOllama stub = new StubOllama(200, envelope(validStatus(), null), 0)) {
            OllamaIntentClient client = new OllamaIntentClient(mapper, parser, false,
                    stub.baseUrl(), "qwen3:4b", Duration.ofSeconds(1), Duration.ofSeconds(1));
            assertFalse(client.enabled());
            assertThrows(LlmIntentException.class, () -> client.classify("Check tuition"));
            assertEquals(0, stub.calls.get());
        }
    }

    private void assertRejected(String content) throws Exception {
        try (StubOllama stub = new StubOllama(200, envelope(content, null), 0)) {
            assertThrows(LlmIntentException.class,
                    () -> client(stub.baseUrl(), Duration.ofSeconds(2)).classify("Test intent"));
        }
    }

    private OllamaIntentClient client(String baseUrl, Duration requestTimeout) {
        return new OllamaIntentClient(mapper, parser, true, baseUrl, "qwen3:4b",
                Duration.ofSeconds(1), requestTimeout);
    }

    private String envelope(String content, String thinking) throws Exception {
        var message = new java.util.LinkedHashMap<String, Object>();
        message.put("role", "assistant");
        message.put("content", content);
        if (thinking != null) message.put("thinking", thinking);
        return mapper.writeValueAsString(Map.of(
                "model", "qwen3:4b", "message", message, "done", true,
                "done_reason", "stop", "total_duration", 123456L));
    }

    private static String validStatus() {
        return """
                {"intent":"CHECK_TUITION_STATUS","channelPreference":"NONE",
                 "confidence":0.88,"clarificationCode":"NONE"}
                """;
    }

    private static final class StubOllama implements AutoCloseable {
        private final HttpServer server;
        private final int status;
        private final String response;
        private final long delayMillis;
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<String> lastBody = new AtomicReference<>();

        private StubOllama(int status, String response, long delayMillis) throws IOException {
            this.status = status;
            this.response = response;
            this.delayMillis = delayMillis;
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/chat", this::handle);
            server.start();
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private void handle(HttpExchange exchange) throws IOException {
            calls.incrementAndGet();
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            try {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, body.length);
                exchange.getResponseBody().write(body);
            } catch (IOException ignored) {
                // Expected when the client timeout closes the local test connection.
            } finally {
                exchange.close();
            }
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
