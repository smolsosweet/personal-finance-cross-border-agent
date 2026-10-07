package com.example.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Production chatbot provider is Gemini only. An explicit disabled mode remains for tests/offline use. */
@Configuration(proxyBeanMethods = false)
public class LlmProviderConfiguration {
    @Bean
    LlmIntentClient llmIntentClient(ObjectMapper mapper, StrictLlmIntentParser parser,
            @Value("${finbridge.llm.enabled:false}") boolean enabled,
            @Value("${finbridge.llm.gemini-api-key:}") String apiKey,
            @Value("${finbridge.llm.model:gemini-3.5-flash-lite}") String model,
            @Value("${finbridge.llm.connect-timeout:10s}") Duration connectTimeout,
            @Value("${finbridge.llm.request-timeout:30s}") Duration requestTimeout) {
        if (!enabled) return new DisabledLlmIntentClient();
        return new GeminiIntentClient(mapper, parser, true, apiKey, model, connectTimeout, requestTimeout);
    }

    private static final class DisabledLlmIntentClient implements LlmIntentClient {
        @Override public boolean enabled() { return false; }
        @Override public LlmIntent classify(String userMessage) {
            throw new LlmIntentException("Gemini intent provider is disabled");
        }
    }
}
