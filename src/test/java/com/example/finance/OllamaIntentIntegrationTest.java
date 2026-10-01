package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ollama_intent_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "finbridge.llm.enabled=true",
        "finbridge.llm.provider=ollama",
        "finbridge.llm.model=qwen3:4b"
})
class OllamaIntentIntegrationTest {
    private static final AtomicInteger CALLS = new AtomicInteger();
    private static final HttpServer SERVER = startServer();

    @Autowired PhaseFourService phaseFour;
    @Autowired DemoDataService demoData;
    @Autowired JdbcTemplate db;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("finbridge.llm.base-url",
                () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
        registry.add("finbridge.llm.request-timeout", () -> "2s");
    }

    @BeforeEach
    void reset() {
        CALLS.set(0);
        demoData.resetAll();
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @Test
    void validOllamaIntentCreatesOnlyAnAwaitingApprovalPlan() {
        String response = phaseFour.sendMessage(
                "Hãy chuẩn bị phương án học phí rẻ nhất cho tôi");
        PhaseFourService.ActionPlan plan = phaseFour.latestAction();

        assertEquals(1, CALLS.get());
        assertNotNull(plan);
        assertTrue(response.contains(plan.id()));
        assertEquals("AWAITING_APPROVAL", plan.status());
        assertEquals("APPROVAL", plan.requiredPermission());
        assertEquals(0, phaseFour.sandboxTransactionCount());
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM sandbox_ledger_entries", Integer.class));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM approvals", Integer.class));
    }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/chat", OllamaIntentIntegrationTest::respond);
            server.start();
            return server;
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        CALLS.incrementAndGet();
        exchange.getRequestBody().readAllBytes();
        String content = "{\"intent\":\"CREATE_TUITION_PLAN\",\"channelPreference\":\"CHEAPEST\","
                + "\"confidence\":0.95,\"clarificationCode\":\"NONE\"}";
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"");
        byte[] body = ("{\"model\":\"qwen3:4b\",\"message\":{\"role\":\"assistant\","
                + "\"content\":\"" + escaped + "\",\"thinking\":\"not used\"},\"done\":true}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
