package com.example.finance;

public interface LlmIntentClient {
    boolean enabled();

    LlmIntent classify(String userMessage);
}
