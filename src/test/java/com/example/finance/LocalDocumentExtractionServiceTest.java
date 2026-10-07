package com.example.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LocalDocumentExtractionServiceTest {
    @Test
    void parsesVietnameseDongGroupingWithoutTreatingDotsAsDecimals() {
        assertEquals("244000", LocalDocumentExtractionService.normalizeAmount("244.000đ", "VND"));
        assertEquals("1250000", LocalDocumentExtractionService.normalizeAmount("1.250.000 VND", "VND"));
        assertEquals("1250000", LocalDocumentExtractionService.normalizeAmount("1,250,000 ₫", "VND"));
    }

    @Test
    void rejectsAmountsThatDoNotMatchVietnameseDongGrouping() {
        assertEquals("", LocalDocumentExtractionService.normalizeAmount("about 244.000", "VND"));
        assertEquals("", LocalDocumentExtractionService.normalizeAmount("0", "VND"));
    }
}
