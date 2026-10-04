package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LlmProviderConfigurationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final StrictLlmIntentParser parser = new StrictLlmIntentParser(mapper);
    private final LlmProviderConfiguration configuration = new LlmProviderConfiguration();

    @Test
    void providerSelectionIsExplicitAndOllamaDefaultsToQwen() {
        LlmIntentClient ollama = select(true, "ollama", "", "", "http://localhost:11434");
        assertInstanceOf(OllamaIntentClient.class, ollama);
        assertEquals("qwen3:4b", ((OllamaIntentClient) ollama)
                .requestBody("test").get("model"));

        LlmIntentClient openai = select(true, "openai", "test-key", "test-model", "http://localhost:11434");
        assertInstanceOf(OpenAiIntentClient.class, openai);
        assertTrue(openai.enabled());

        LlmIntentClient disabled = select(true, "disabled", "test-key", "test-model", "http://localhost:11434");
        assertFalse(disabled.enabled());
    }

    @Test
    void apiKeyAloneDoesNotEnableOpenAiAndUnknownProviderFailsClearly() {
        LlmIntentClient openai = select(false, "openai", "test-key", "test-model", "http://localhost:11434");
        assertFalse(openai.enabled());
        assertThrows(LlmIntentException.class, () -> openai.classify("Check tuition"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> select(false, "unknown", "", "", "http://localhost:11434"));
        assertTrue(error.getMessage().contains("expected ollama, gemini, openai, or disabled"));
    }

    private LlmIntentClient select(boolean enabled, String provider, String key, String model, String baseUrl) {
        return configuration.llmIntentClient(mapper, parser, enabled, provider, key, "", model, baseUrl,
                Duration.ofSeconds(1), Duration.ofSeconds(2));
    }

    @Test void geminiRequiresItsOwnKeyAndExplicitModelOnlyWhenEnabled() {
        assertThrows(IllegalStateException.class, () -> select(true,"gemini","openai-only","gemini-3.5-flash-lite","http://localhost:11434"));
        assertThrows(IllegalStateException.class, () -> configuration.llmIntentClient(mapper,parser,true,"gemini","","synthetic-key","","",Duration.ofSeconds(1),Duration.ofSeconds(2)));
        var client=configuration.llmIntentClient(mapper,parser,true,"gemini","","synthetic-key","gemini-3.5-flash-lite","http://localhost:11434",Duration.ofSeconds(1),Duration.ofSeconds(2));
        assertInstanceOf(GeminiIntentClient.class,client);assertTrue(client.enabled());
        assertFalse(select(false,"gemini","","","http://localhost:11434").enabled());
    }
}
