package com.example.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LlmProviderConfiguration {
    @Bean
    LlmIntentClient llmIntentClient(ObjectMapper mapper, StrictLlmIntentParser parser,
            @Value("${finbridge.llm.enabled:false}") boolean enabled,
            @Value("${finbridge.llm.provider:openai}") String configuredProvider,
            @Value("${finbridge.llm.api-key:}") String apiKey,
            @Value("${finbridge.llm.model:}") String configuredModel,
            @Value("${finbridge.llm.base-url:http://localhost:11434}") String baseUrl,
            @Value("${finbridge.llm.connect-timeout:3s}") Duration connectTimeout,
            @Value("${finbridge.llm.request-timeout:60s}") Duration requestTimeout) {
        String provider = configuredProvider == null ? "" : configuredProvider.trim().toLowerCase(Locale.ROOT);
        String model = configuredModel == null ? "" : configuredModel.trim();
        return switch (provider) {
            case "disabled" -> new DisabledLlmIntentClient();
            case "openai" -> new OpenAiIntentClient(
                    mapper, parser, enabled, apiKey, model, connectTimeout, requestTimeout);
            case "ollama" -> new OllamaIntentClient(
                    mapper, parser, enabled, baseUrl,
                    model.isBlank() ? "qwen3:4b" : model, connectTimeout, requestTimeout);
            default -> throw new IllegalStateException(
                    "Unknown FINBRIDGE_LLM_PROVIDER '" + configuredProvider
                            + "'; expected ollama, openai, or disabled");
        };
    }

    private static final class DisabledLlmIntentClient implements LlmIntentClient {
        @Override
        public boolean enabled() {
            return false;
        }

        @Override
        public LlmIntent classify(String userMessage) {
            throw new LlmIntentException("LLM intent provider is disabled");
        }
    }
}
