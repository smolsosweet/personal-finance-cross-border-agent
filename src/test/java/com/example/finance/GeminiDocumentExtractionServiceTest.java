package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

class GeminiDocumentExtractionServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void transactionReceiptIsSentToGeminiAndValidatedForExistingReviewForm() throws Exception {
        try (var stub = new Stub("""
                {"merchant":"MUJI","amount":"244.000","currency":"vnd","occurredAt":"2026-10-03",
                 "paymentMethod":"Momo","category":"Shopping","reference":"534428"}
                """)) {
            var file = new MockMultipartFile("file", "receipt.png", "image/png", new byte[]{1, 2, 3});
            var result = service(stub).extract(file, "transaction");
            assertEquals("gemini", result.provider());
            assertEquals("244000", result.fields().get("amount"));
            assertEquals("VND", result.fields().get("currency"));
            assertEquals("2026-10-03", result.fields().get("occurredAt"));
            assertEquals("MUJI", result.fields().get("merchant"));
            assertEquals("Shopping", result.fields().get("category"));
            assertEquals("synthetic-test-key", stub.apiKey);
            assertEquals(1, stub.calls);
            var request = mapper.readTree(stub.requestBody);
            var parts = request.path("contents").get(0).path("parts");
            assertTrue(parts.get(0).path("text").asText().contains("Extract merchant"));
            assertEquals("image/png", parts.get(1).path("inlineData").path("mimeType").asText());
            assertEquals(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}),
                    parts.get(1).path("inlineData").path("data").asText());
        }
    }

    @Test void educationBillAlsoUsesGeminiVisionAndLeavesMissingFieldsBlank() throws Exception {
        try (var stub = new Stub("""
                {"institution":"Shenzhen University","amount":"1250000","currency":"VND",
                 "dueDate":"2026-10-30","paymentReference":"INV-345","recipientName":"Student Name"}
                """)) {
            var file = new MockMultipartFile("file", "bill.webp", "image/webp", new byte[]{4, 5, 6});
            var result = service(stub).extract(file, "student-bill");
            assertEquals("gemini", result.provider());
            assertEquals("Shenzhen University", result.fields().get("institution"));
            assertEquals("1250000", result.fields().get("amount"));
            assertEquals("2026-10-30", result.fields().get("dueDate"));
            assertEquals("INV-345", result.fields().get("paymentReference"));
            assertEquals(1, stub.calls);
            assertTrue(stub.requestBody.contains("dueDate"));
        }
    }

    @Test void absentApiKeyFailsBeforeAnyRequestAndDoesNotExposeValues() throws Exception {
        try (var stub = new Stub("{}")) {
            var service = new GeminiDocumentExtractionService(mapper, "", "gemini-3.5-flash-lite",
                    Duration.ofSeconds(1), stub.endpoint());
            var file = new MockMultipartFile("file", "receipt.png", "image/png", new byte[]{1});
            var error = assertThrows(IllegalStateException.class, () -> service.extract(file, "transaction"));
            assertTrue(error.getMessage().contains("GEMINI_API_KEY"));
            assertEquals(0, stub.calls);
        }
    }

    @Test void existingExtractionEndpointRoutesTheReceiptToGemini() throws Exception {
        try (var stub = new Stub("""
                {"merchant":"Cafe","amount":"50000","currency":"VND","occurredAt":"2026-10-07",
                 "paymentMethod":"","category":"Food & Drinks","reference":""}
                """)) {
            var file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", new byte[]{7, 8, 9});
            ResponseEntity<?> response = new DocumentExtractionController(service(stub)).extract(file, "transaction");
            assertEquals(200, response.getStatusCode().value());
            assertInstanceOf(DocumentExtractionService.Result.class, response.getBody());
            assertEquals("gemini", ((DocumentExtractionService.Result) response.getBody()).provider());
            assertEquals(1, stub.calls);
        }
    }

    private GeminiDocumentExtractionService service(Stub stub) {
        return new GeminiDocumentExtractionService(mapper, "synthetic-test-key", "gemini-3.5-flash-lite",
                Duration.ofSeconds(2), stub.baseEndpoint());
    }

    private static final class Stub implements AutoCloseable {
        private final HttpServer server;
        private final String result;
        private volatile String requestBody;
        private volatile String apiKey;
        private volatile int calls;

        private Stub(String result) throws Exception {
            this.result = result;
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1beta/models/gemini-3.5-flash-lite:generateContent", exchange -> {
                calls++;
                apiKey = exchange.getRequestHeaders().getFirst("x-goog-api-key");
                requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String response = new ObjectMapper().writeValueAsString(Map.of("candidates", List.of(Map.of(
                        "finishReason", "STOP", "content", Map.of("parts", List.of(Map.of("text", this.result)))))));
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
        }

        URI endpoint() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta/models/gemini-3.5-flash-lite:generateContent"); }
        URI baseEndpoint() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta/models/"); }
        @Override public void close() { server.stop(0); }
    }
}
