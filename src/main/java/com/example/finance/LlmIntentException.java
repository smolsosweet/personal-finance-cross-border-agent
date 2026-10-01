package com.example.finance;

public class LlmIntentException extends RuntimeException {
    public LlmIntentException(String message) {
        super(message);
    }

    public LlmIntentException(String message, Throwable cause) {
        super(message, cause);
    }
}
