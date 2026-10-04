package com.example.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import static com.example.finance.GeminiProviderException.Reason.*;

/** Backend-only generateContent adapter. No tools, financial payload, retries or provider switching. */
public final class GeminiIntentClient implements LlmIntentClient {
    private final ObjectMapper mapper;
    private final StrictLlmIntentParser parser;
    private final boolean requestedEnabled;
    private final String apiKey;
    private final URI endpoint;
    private final Duration requestTimeout;
    private final HttpClient http;

    public GeminiIntentClient(ObjectMapper mapper, StrictLlmIntentParser parser, boolean enabled,
            String apiKey, String model, Duration connectTimeout, Duration requestTimeout) {
        this(mapper, parser, enabled, apiKey, model, connectTimeout, requestTimeout, null);
    }

    // A local HTTP endpoint is injected only by tests; production always uses Google's HTTPS endpoint.
    GeminiIntentClient(ObjectMapper mapper, StrictLlmIntentParser parser, boolean enabled,
            String apiKey, String model, Duration connectTimeout, Duration requestTimeout, URI testEndpoint) {
        this.mapper = mapper;
        this.parser = parser;
        this.requestedEnabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        String configuredModel = model == null ? "" : model.trim();
        if (enabled && this.apiKey.isBlank())
            throw new IllegalStateException("Gemini configuration requires inherited GEMINI_API_KEY (value is never displayed)");
        if (enabled && !configuredModel.matches("gemini-[A-Za-z0-9._-]+"))
            throw new IllegalStateException("Gemini configuration requires an explicit valid FINBRIDGE_LLM_MODEL");
        this.endpoint = testEndpoint != null ? testEndpoint : URI.create(
                "https://generativelanguage.googleapis.com/v1beta/models/" + configuredModel + ":generateContent");
        this.requestTimeout = positive(requestTimeout);
        this.http = HttpClient.newBuilder().connectTimeout(positive(connectTimeout))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override public boolean enabled() { return requestedEnabled; }

    @Override public LlmIntent classify(String userMessage) {
        if (!enabled()) throw new GeminiProviderException(DISABLED);
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody(userMessage))))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 401 || status == 403) throw new GeminiProviderException(AUTHENTICATION);
            if (status == 404) throw new GeminiProviderException(MODEL_UNAVAILABLE);
            if (status == 429) throw new GeminiProviderException(QUOTA_EXHAUSTED);
            if (status >= 500) throw new GeminiProviderException(SERVICE_UNAVAILABLE);
            if (status != 200) throw new GeminiProviderException(REQUEST_REJECTED);
            try { return parser.parse(extractContent(mapper.readTree(response.body()))); }
            catch (GeminiProviderException ex) { throw ex; }
            catch (Exception ex) { throw new GeminiProviderException(INVALID_OUTPUT); }
        } catch (HttpTimeoutException ex) {
            throw new GeminiProviderException(TIMEOUT);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GeminiProviderException(SERVICE_UNAVAILABLE);
        } catch (GeminiProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            // Do not retain causes: HTTP/JSON exception messages may contain headers or provider output.
            throw new GeminiProviderException(SERVICE_UNAVAILABLE);
        }
    }

    Map<String, Object> requestBody(String question) {
        return Map.of("systemInstruction", Map.of("parts", List.of(Map.of("text", ModelConversationContext.instructions()))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", question)))),
                "generationConfig", Map.of("candidateCount", 1, "maxOutputTokens", 512,
                        "responseMimeType", "application/json", "responseJsonSchema", LlmIntentContract.schema()));
    }

    String extractContent(JsonNode response) {
        if (response == null || !response.isObject() || response.has("error")) throw new GeminiProviderException(INVALID_OUTPUT);
        var feedback = response.path("promptFeedback");
        if (feedback.hasNonNull("blockReason") && !"BLOCK_REASON_UNSPECIFIED".equals(feedback.path("blockReason").asText()))
            throw new GeminiProviderException(OUTPUT_BLOCKED);
        var candidates = response.path("candidates");
        if (!candidates.isArray() || candidates.size() != 1) throw new GeminiProviderException(INVALID_OUTPUT);
        var candidate = candidates.get(0);
        String finish = candidate.path("finishReason").asText();
        if ("MAX_TOKENS".equals(finish)) throw new GeminiProviderException(OUTPUT_TRUNCATED);
        if (List.of("SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "IMAGE_SAFETY").contains(finish))
            throw new GeminiProviderException(OUTPUT_BLOCKED);
        if (!"STOP".equals(finish)) throw new GeminiProviderException(INVALID_OUTPUT);
        var safety = candidate.path("safetyRatings");
        if (safety.isArray()) for (var rating : safety)
            if (rating.path("blocked").asBoolean(false)) throw new GeminiProviderException(OUTPUT_BLOCKED);
        var content = candidate.path("content");
        var parts = content.path("parts");
        if (!"model".equals(content.path("role").asText()) || !parts.isArray() || parts.size() != 1)
            throw new GeminiProviderException(INVALID_OUTPUT);
        var part = parts.get(0);
        if (!part.isObject() || !part.path("text").isTextual() || part.path("text").asText().isBlank()
                || part.path("thought").asBoolean(false)) throw new GeminiProviderException(INVALID_OUTPUT);
        var fields = part.fieldNames();
        while (fields.hasNext()) if (!List.of("text", "thought", "thoughtSignature").contains(fields.next()))
            throw new GeminiProviderException(INVALID_OUTPUT);
        return part.path("text").textValue();
    }

    private static Duration positive(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero())
            throw new IllegalArgumentException("Gemini connect/request timeouts must be positive");
        return timeout;
    }
}
