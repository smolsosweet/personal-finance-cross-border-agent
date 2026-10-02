package com.example.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class OpenAiIntentClient implements LlmIntentClient {
    private static final URI RESPONSES_ENDPOINT = URI.create("https://api.openai.com/v1/responses");

    private final ObjectMapper mapper;
    private final StrictLlmIntentParser parser;
    private final boolean requestedEnabled;
    private final String apiKey;
    private final String model;
    private final Duration requestTimeout;
    private final HttpClient http;

    public OpenAiIntentClient(ObjectMapper mapper, StrictLlmIntentParser parser,
            boolean requestedEnabled, String apiKey, String model) {
        this(mapper, parser, requestedEnabled, apiKey, model,
                Duration.ofSeconds(3), Duration.ofSeconds(8));
    }

    public OpenAiIntentClient(ObjectMapper mapper, StrictLlmIntentParser parser,
            boolean requestedEnabled, String apiKey, String model,
            Duration connectTimeout, Duration requestTimeout) {
        this.mapper = mapper;
        this.parser = parser;
        this.requestedEnabled = requestedEnabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
        this.requestTimeout = requestTimeout;
        this.http = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    }

    @Override
    public boolean enabled() {
        return requestedEnabled && !apiKey.isBlank() && !model.isBlank();
    }

    @Override
    public LlmIntent classify(String userMessage) {
        if (!enabled()) throw new LlmIntentException("OpenAI intent provider is not configured");
        try {
            String requestJson = mapper.writeValueAsString(requestBody(userMessage));
            HttpRequest request = HttpRequest.newBuilder(RESPONSES_ENDPOINT)
                    .timeout(requestTimeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new LlmIntentException("OpenAI intent provider returned a non-success status");
            }
            return parser.parse(extractOutputText(mapper.readTree(response.body())));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LlmIntentException("OpenAI intent provider request was interrupted", ex);
        } catch (LlmIntentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LlmIntentException("OpenAI intent provider was unavailable", ex);
        }
    }

    Map<String, Object> requestBody(String userMessage) {
        return Map.of(
                "model", model,
                "instructions", ModelConversationContext.instructions(),
                "input", java.util.List.of(Map.of(
                        "role", "user",
                        "content", java.util.List.of(Map.of("type", "input_text", "text", userMessage)))),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "finbridge_intent",
                        "strict", true,
                        "schema", LlmIntentContract.schema())),
                "max_output_tokens", 160,
                "store", false);
    }

    String extractOutputText(JsonNode response) {
        if (response == null || !"completed".equals(response.path("status").asText())) {
            throw new LlmIntentException("OpenAI intent response was incomplete");
        }
        for (JsonNode output : response.path("output")) {
            if (!"message".equals(output.path("type").asText())) continue;
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())
                        && content.path("text").isTextual()) {
                    return content.path("text").textValue();
                }
                if ("refusal".equals(content.path("type").asText())) {
                    throw new LlmIntentException("OpenAI intent response was refused");
                }
            }
        }
        throw new LlmIntentException("OpenAI intent response contained no structured output");
    }
}
