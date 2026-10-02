package com.example.finance;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
class ChatViewAdvice {
    private final Duration timeout;
    ChatViewAdvice(@Value("${finbridge.llm.request-timeout:60s}") Duration timeout) { this.timeout = timeout; }
    @ModelAttribute("chatTimeoutMillis")
    long timeoutMillis() { return timeout.toMillis() + 10000; }
}
