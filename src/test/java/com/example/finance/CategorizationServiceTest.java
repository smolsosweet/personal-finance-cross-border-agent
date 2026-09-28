package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CategorizationServiceTest {
    private final CategorizationService service = new CategorizationService();

    @Test void thresholdBoundariesAreExact() {
        assertEquals("AUTO", CategorizationService.statusFor(100));
        assertEquals("AUTO", CategorizationService.statusFor(90));
        assertEquals("CONFIRMATION_REQUIRED", CategorizationService.statusFor(89));
        assertEquals("CONFIRMATION_REQUIRED", CategorizationService.statusFor(60));
        assertEquals("PURPOSE_REQUIRED", CategorizationService.statusFor(59));
        assertEquals("PURPOSE_REQUIRED", CategorizationService.statusFor(0));
    }

    @Test void deterministicRulesProduceAllConfidenceLevels() {
        var high = service.categorize(transaction("Highlands Coffee", "Expense"));
        var medium = service.categorize(transaction("Campus Store", "Expense"));
        var low = service.categorize(transaction("Unknown QR Merchant", "Expense"));

        assertEquals("Food & Drinks", high.category());
        assertEquals(97, high.confidence());
        assertEquals("AUTO", high.reviewStatus());

        assertEquals("Shopping", medium.category());
        assertEquals(72, medium.confidence());
        assertEquals("CONFIRMATION_REQUIRED", medium.reviewStatus());

        assertNull(low.category());
        assertEquals(35, low.confidence());
        assertEquals("PURPOSE_REQUIRED", low.reviewStatus());
    }

    @Test void transactionTypesHaveDeterministicCategories() {
        assertEquals("Income", service.categorize(transaction("Employer", "Income")).category());
        assertEquals("Transfer", service.categorize(transaction("Own Account", "Internal Transfer")).category());
        assertEquals("Refund", service.categorize(transaction("Highlands", "Refund")).category());
    }

    private static TransactionService.Normalized transaction(String merchant, String type) {
        return new TransactionService.Normalized(merchant, "Demo", new BigDecimal("100.00"), "VND", "OUT", type, LocalDateTime.now());
    }
}
