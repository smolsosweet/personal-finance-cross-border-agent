package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:phase2_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class PhaseTwoIntegrationTest {
    @Autowired TransactionService service;

    @BeforeEach void reset() { service.reset(); }

    @Test void highConfidenceIsAutomatic() {
        Map<String,Object> tx = service.transaction(service.simulate("high"));
        assertEquals("Food & Drinks", tx.get("category"));
        assertEquals(97, tx.get("confidence"));
        assertEquals("AUTO", tx.get("review_status"));
    }

    @Test void mediumConfidenceRequiresConfirmation() {
        String id = service.simulate("medium");
        Map<String,Object> suggested = service.transaction(id);
        assertEquals("Shopping", suggested.get("category"));
        assertEquals(72, suggested.get("confidence"));
        assertEquals("CONFIRMATION_REQUIRED", suggested.get("review_status"));

        service.confirmCategory(id, "Groceries");
        Map<String,Object> confirmed = service.transaction(id);
        assertEquals("Groceries", confirmed.get("category"));
        assertEquals("Shopping", confirmed.get("previous_category"));
        assertEquals("CONFIRMED", confirmed.get("review_status"));

        service.undoCategory(id);
        Map<String,Object> undone = service.transaction(id);
        assertEquals("Shopping", undone.get("category"));
        assertEquals("CONFIRMATION_REQUIRED", undone.get("review_status"));
    }

    @Test void lowConfidenceRequiresPurpose() {
        String id = service.simulate("low");
        Map<String,Object> tx = service.transaction(id);
        assertNull(tx.get("category"));
        assertEquals(35, tx.get("confidence"));
        assertEquals("PURPOSE_REQUIRED", tx.get("review_status"));

        service.confirmCategory(id, "Study materials");
        assertEquals("Study materials", service.transaction(id).get("category"));
        assertEquals("CONFIRMED", service.transaction(id).get("review_status"));
    }

    @Test void productionReviewRequiresPurposeAndStoresUserDecision() {
        String id = service.simulate("low");

        assertThrows(IllegalArgumentException.class,
                () -> service.reviewTransaction(id, "Shopping", null));

        service.reviewTransaction(id, "Shopping", "Lunch with classmates");
        Map<String,Object> reviewed = service.transaction(id);
        assertEquals("Shopping", reviewed.get("category"));
        assertEquals("Lunch with classmates", reviewed.get("purpose"));
        assertEquals("USER", reviewed.get("category_source"));
        assertEquals("CONFIRMED", reviewed.get("review_status"));
        assertNotNull(reviewed.get("reviewed_at"));
    }

    @Test void customCategoryCanBeCreatedInlineAndArchivedWithoutChangingHistory() {
        String id = service.simulate("low");
        service.reviewTransaction(id, null, "Pet care", "Bought cat food");

        assertEquals("Pet care", service.transaction(id).get("category"));
        assertTrue(service.categories().stream().anyMatch(category ->
                "Pet care".equals(category.get("name"))
                        && "CUSTOM".equals(category.get("category_type"))
                        && Boolean.TRUE.equals(category.get("active"))));

        service.archiveCustomCategory("Pet care");
        assertEquals("Pet care", service.transaction(id).get("category"));
        assertTrue(service.categories().stream().anyMatch(category ->
                "Pet care".equals(category.get("name")) && Boolean.FALSE.equals(category.get("active"))));

        String next = service.simulate("low");
        assertThrows(IllegalArgumentException.class,
                () -> service.reviewTransaction(next, "Pet care", "Another purchase"));
    }

    @Test void systemCategoryCannotBeArchived() {
        assertThrows(IllegalArgumentException.class,
                () -> service.archiveCustomCategory("Shopping"));
    }

    @Test void automaticCategoryCanBeUndone() {
        String id = service.simulate("high");
        service.undoCategory(id);
        Map<String,Object> tx = service.transaction(id);
        assertNull(tx.get("category"));
        assertEquals("PURPOSE_REQUIRED", tx.get("review_status"));
        assertEquals("Category change undone by user", tx.get("categorization_evidence"));
    }

    @Test void budgetUsesOnlyConfirmedAndAutomaticCategories() {
        BigDecimal before = spentFor("Shopping");
        String id = service.simulate("medium");
        assertEquals(0, before.compareTo(spentFor("Shopping")));

        service.confirmCategory(id, "Shopping");
        assertEquals(0, before.add(new BigDecimal("120000.00")).compareTo(spentFor("Shopping")));
    }

    @Test void dashboardAndFeedAreGroundedInStoredData() {
        Map<String,Object> before = service.dashboard();
        service.simulate("transfer");
        Map<String,Object> afterTransfer = service.dashboard();
        assertEquals(0, ((BigDecimal)before.get("income")).compareTo((BigDecimal)afterTransfer.get("income")));
        assertEquals(0, ((BigDecimal)before.get("expenses")).compareTo((BigDecimal)afterTransfer.get("expenses")));

        service.simulate("low");
        assertTrue((Integer)service.dashboard().get("pendingReview") > (Integer)before.get("pendingReview"));
        assertTrue(service.proactiveFeed().stream().anyMatch(item -> "HIGH".equals(item.get("priority"))));
        assertTrue(service.proactiveFeed().stream().allMatch(item -> item.get("evidence") != null));
    }

    private BigDecimal spentFor(String category) {
        return (BigDecimal) service.budgetSummary().stream()
                .filter(item -> category.equals(item.get("category")))
                .findFirst().orElseThrow().get("spent");
    }
}
