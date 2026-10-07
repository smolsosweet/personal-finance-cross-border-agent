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
    void enabledChatbotAlwaysUsesGeminiAndDisabledModeMakesNoProviderChoice() {
        LlmIntentClient gemini = select(true, "synthetic-gemini-key", "gemini-3.5-flash-lite");
        assertInstanceOf(GeminiIntentClient.class, gemini);
        assertTrue(gemini.enabled());

        LlmIntentClient disabled = select(false, "", "");
        assertFalse(disabled.enabled());
    }

    @Test
    void enabledGeminiRequiresServerKeyAndValidModel() {
        assertThrows(IllegalStateException.class, () -> select(true, "", "gemini-3.5-flash-lite"));
        assertThrows(IllegalStateException.class, () -> select(true, "synthetic-key", "bad/model"));
    }

    private LlmIntentClient select(boolean enabled, String key, String model) {
        return configuration.llmIntentClient(mapper, parser, enabled, key, model,
                Duration.ofSeconds(1), Duration.ofSeconds(2));
    }

    @Test void geminiRequiresItsOwnKeyAndExplicitModelOnlyWhenEnabled() {
        assertThrows(IllegalStateException.class, () -> select(true,"","gemini-3.5-flash-lite"));
        assertThrows(IllegalStateException.class, () -> select(true,"synthetic-key",""));
        var client=configuration.llmIntentClient(mapper,parser,true,"synthetic-key","gemini-3.5-flash-lite",Duration.ofSeconds(1),Duration.ofSeconds(2));
        assertInstanceOf(GeminiIntentClient.class,client);assertTrue(client.enabled());
        assertFalse(select(false,"","").enabled());
    }
}
