package com.example.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class OllamaIntentClient implements LlmIntentClient {
    private final ObjectMapper mapper;
    private final StrictLlmIntentParser parser;
    private final boolean requestedEnabled;
    private final String model;
    private final URI chatEndpoint;
    private final Duration requestTimeout;
    private final HttpClient http;

    public OllamaIntentClient(ObjectMapper mapper, StrictLlmIntentParser parser,
            boolean requestedEnabled, String baseUrl, String model,
            Duration connectTimeout, Duration requestTimeout) {
        this.mapper = mapper;
        this.parser = parser;
        this.requestedEnabled = requestedEnabled;
        this.model = model == null ? "" : model.trim();
        this.chatEndpoint = chatEndpoint(baseUrl);
        this.requestTimeout = positive(requestTimeout, "request timeout");
        this.http = HttpClient.newBuilder()
                .connectTimeout(positive(connectTimeout, "connect timeout"))
                .build();
    }

    @Override
    public boolean enabled() {
        return requestedEnabled && !model.isBlank();
    }

    @Override
    public LlmIntent classify(String userMessage) {
        if (!enabled()) throw new LlmIntentException("Ollama intent provider is not configured");
        try {
            String requestJson = mapper.writeValueAsString(requestBody(userMessage));
            HttpRequest request = HttpRequest.newBuilder(chatEndpoint)
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new LlmIntentException("Ollama intent provider returned a non-success status");
            }
            return parser.parse(extractContent(mapper.readTree(response.body())));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LlmIntentException("Ollama intent provider request was interrupted", ex);
        } catch (LlmIntentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LlmIntentException("Ollama intent provider was unavailable", ex);
        }
    }

    Map<String, Object> requestBody(String userMessage) {
        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", ModelConversationContext.instructions()),
                        Map.of("role", "user", "content", userMessage)),
                "stream", false,
                "keep_alive", "10m",
                "think", false,
                "format", LlmIntentContract.schema(),
                "options", Map.of("temperature", 0, "num_predict", 96));
    }

    String extractContent(JsonNode response) {
        if (response == null || !response.path("done").asBoolean(false)) {
            throw new LlmIntentException("Ollama intent response was incomplete");
        }
        JsonNode content = response.path("message").path("content");
        if (!content.isTextual()) {
            throw new LlmIntentException("Ollama intent response contained no structured content");
        }
        return content.textValue();
    }

    private static URI chatEndpoint(String baseUrl) {
        try {
            String normalized = baseUrl == null ? "" : baseUrl.trim();
            if (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
            URI endpoint = URI.create(normalized + "/api/chat");
            if (!("http".equalsIgnoreCase(endpoint.getScheme())
                    || "https".equalsIgnoreCase(endpoint.getScheme())) || endpoint.getHost() == null) {
                throw new IllegalArgumentException("Ollama base URL must be an HTTP(S) URL");
            }
            return endpoint;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid FINBRIDGE_LLM_BASE_URL", ex);
        }
    }

    private static Duration positive(Duration duration, String label) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("LLM " + label + " must be positive");
        }
        return duration;
    }
}
