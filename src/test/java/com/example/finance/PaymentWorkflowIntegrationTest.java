package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:payment_workflow_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class PaymentWorkflowIntegrationTest {
    @Autowired CrossBorderService crossBorder;
    @Autowired PhaseFourService phaseFour;
    @Autowired DemoDataService demoData;
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;

    @BeforeEach void reset() {
        demoData.resetAll();
    }

    @Test void selectedPendingPlanDoesNotShowAnotherActionsReceiptOrAudit() throws Exception {
        addVerifiedExpense("First completed fee", "100.00", "RECEIPT-FIRST");
        var completed = createLocked("BANK_A");
        var previousReceipt = phaseFour.approveAndExecute(completed.id());
        assertNotNull(previousReceipt);

        addVerifiedExpense("Second pending fee", "200.00", "PENDING-SECOND");
        var pending = createLocked("VCB");
        var model = mvc.perform(get("/").param("action", pending.id()))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel();

        assertEquals(pending.id(), ((PhaseFourService.ActionPlan) model.get("latestAction")).id());
        assertNull(model.get("latestReceipt"), "A new plan must not inherit the last global receipt");
        assertNull(phaseFour.receiptForAction(pending.id()));
        assertEquals(previousReceipt.transactionId(), phaseFour.receiptForAction(completed.id()).transactionId());
        @SuppressWarnings("unchecked")
        var events = (List<PhaseFourService.AuditEvent>) model.get("auditEvents");
        assertFalse(events.isEmpty());
        assertTrue(events.stream().allMatch(event -> pending.id().equals(event.referenceId())));
        assertFalse(events.stream().anyMatch(event -> "PAYMENT_RECEIPT_CREATED".equals(event.eventType())));
    }

    @Test void completedPlanRemainsViewableAfterSelectingAnotherBillAndRefreshingQuotes() throws Exception {
        int paidExpense = addVerifiedExpense("Paid education fee", "100.00", "PAID-VIEW");
        var completed = createLocked("BANK_A");
        var originalReview = phaseFour.paymentReview(completed);
        var receipt = phaseFour.approveAndExecute(completed.id());
        assertNotNull(receipt);

        int selectedExpense = addVerifiedExpense("Next education fee", "250.00", "NEXT-VIEW");
        crossBorder.refreshQuotes();
        assertEquals(selectedExpense, crossBorder.selectedExpense().id());
        assertNotEquals(completed.quoteId(), quote("BANK_A").quoteId());

        var model = mvc.perform(get("/").param("action", completed.id()))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        var displayedPlan = (PhaseFourService.ActionPlan) model.get("latestAction");
        var displayedReceipt = (PhaseFourService.Receipt) model.get("latestReceipt");
        assertEquals(completed.id(), displayedPlan.id());
        assertEquals(paidExpense, displayedPlan.expenseId());
        assertEquals("COMPLETED", displayedPlan.status());
        assertEquals(receipt.transactionId(), displayedReceipt.transactionId());
        assertEquals(completed.quoteId(), displayedReceipt.quoteId());
        var displayedReview = (PhaseFourService.PaymentReview) model.get("paymentReview");
        assertEquals("Paid education fee", displayedReview.billTitle());
        assertEquals("PAID-VIEW", displayedReview.paymentReference());
        assertEquals(originalReview.recipientName(), displayedReview.recipientName());
        assertEquals(originalReview.recipientBankName(), displayedReview.recipientBankName());
        assertEquals(originalReview.sourceDisplayName(), displayedReview.sourceDisplayName());
        assertEquals(originalReview.quoteSource(), displayedReview.quoteSource());
        assertEquals(originalReview.quotedAt(), displayedReview.quotedAt());
        assertEquals(originalReview.expiresAt(), displayedReview.expiresAt());
        assertEquals(originalReview.latestSafeDate(), displayedReview.latestSafeDate());
        assertMoney(receipt.vndBalanceBefore(), displayedReview.sourceBalance());
        assertMoney(receipt.vndBalanceAfter(), displayedReview.balanceAfter());
        assertTrue(phaseFour.auditEvents(completed.id()).stream()
                .anyMatch(event -> "PAYMENT_RECEIPT_CREATED".equals(event.eventType())
                        && event.details().contains(receipt.transactionId())));
    }

    @Test void oldSelectedBillRequestIsRejectedWithoutCreatingPlan() {
        var oldBill = crossBorder.selectedExpense();
        var oldQuote = quote("BANK_A");
        addVerifiedExpense("New selected fee", "500.00", "NEW-SELECTION");

        assertThrows(IllegalArgumentException.class, () -> phaseFour.createTuitionPlan(
                "BANK_A", oldQuote.sourceAccountId(), oldBill.id(),
                oldBill.updatedAt().toString(), oldQuote.quoteId()));
        assertEquals(0, planCount());
        assertEquals(0, phaseFour.sandboxTransactionCount());
    }

    @Test void changedBillVersionIsRejectedWithoutCreatingPlan() {
        int expenseId = addVerifiedExpense("Versioned fee", "500.00", "VERSION-OLD");
        var oldBill = crossBorder.selectedExpense();
        var oldQuote = quote("BANK_A");
        crossBorder.updateExpense(expenseId, "OTHER", "Revised fee", CrossBorderService.SCHOOL_NAME,
                new BigDecimal("550.00"), "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT_NAME,
                CrossBorderService.SCHOOL_RECIPIENT_BANK, CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE,
                CrossBorderService.SCHOOL_RECIPIENT, "VERSION-NEW", LocalDate.now().plusDays(20));

        assertThrows(IllegalArgumentException.class, () -> phaseFour.createTuitionPlan(
                "BANK_A", oldQuote.sourceAccountId(), expenseId,
                oldBill.updatedAt().toString(), oldQuote.quoteId()));
        assertEquals(0, planCount());
    }

    @Test void oldQuoteAndWrongFundingSourceAreRejectedWithoutCreatingPlan() {
        var bill = crossBorder.selectedExpense();
        var oldQuote = quote("BANK_A");
        crossBorder.refreshQuotes();
        assertThrows(IllegalArgumentException.class, () -> phaseFour.createTuitionPlan(
                "BANK_A", oldQuote.sourceAccountId(), bill.id(),
                bill.updatedAt().toString(), oldQuote.quoteId()));

        var currentQuote = quote("BANK_A");
        assertThrows(IllegalArgumentException.class, () -> phaseFour.createTuitionPlan(
                "BANK_A", "VCB_VND", bill.id(), bill.updatedAt().toString(), currentQuote.quoteId()));
        assertEquals(0, planCount());
        assertEquals(0, phaseFour.sandboxTransactionCount());
    }

    @Test void identicalPendingRequestReusesPlanAndDoesNotCreateApprovalOrPayment() {
        var first = createLocked("BANK_A");
        var repeated = createLocked("BANK_A");

        assertEquals(first.id(), repeated.id());
        assertEquals(first.idempotencyKey(), repeated.idempotencyKey());
        assertEquals("AWAITING_APPROVAL", repeated.status());
        assertEquals("APPROVAL", repeated.requiredPermission());
        assertEquals(1, planCount());
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM approvals", Integer.class));
        assertEquals(0, phaseFour.sandboxTransactionCount());
    }

    @Test void changingChannelInvalidatesPreviousPlanAndItsApproval() {
        addVerifiedExpense("Channel switch fee", "100.00", "CHANNEL-SWITCH");
        var old = createLocked("BANK_A");
        assertEquals("APPROVED", phaseFour.approve(old.id()).status());
        var replacement = createLocked("ALIPAY");

        assertNotEquals(old.id(), replacement.id());
        assertEquals("INVALIDATED", phaseFour.action(old.id()).status());
        assertEquals("AWAITING_APPROVAL", replacement.status());
        assertNull(phaseFour.execute(old.id()));
        assertEquals(0, phaseFour.sandboxTransactionCount());
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM approvals WHERE action_id=? AND status='VALID'",
                Integer.class, old.id()));
    }

    @Test void refreshedQuoteRequiresNewApprovalAndInvalidatesPreviousPlan() {
        var old = createLocked("BANK_A");
        phaseFour.approve(old.id());
        crossBorder.refreshQuotes();
        var replacement = createLocked("BANK_A");

        assertNotEquals(old.quoteId(), replacement.quoteId());
        assertEquals("INVALIDATED", phaseFour.action(old.id()).status());
        assertEquals("AWAITING_APPROVAL", replacement.status());
        assertNull(phaseFour.execute(old.id()));
        assertEquals(0, phaseFour.sandboxTransactionCount());
    }

    @Test void archivedAndCancelledBillsCannotCreatePaymentPlans() {
        int archivedId = addVerifiedExpense("Archived fee", "100.00", "ARCHIVED-FEE");
        var archived = crossBorder.selectedExpense();
        var archivedQuote = quote("BANK_A");
        crossBorder.archiveExpense(archivedId);
        assertThrows(IllegalArgumentException.class, () -> phaseFour.createTuitionPlan(
                "BANK_A", archivedQuote.sourceAccountId(), archivedId,
                archived.updatedAt().toString(), archivedQuote.quoteId()));

        int cancelledId = addVerifiedExpense("Cancelled fee", "100.00", "CANCELLED-FEE");
        var cancelled = crossBorder.selectedExpense();
        var cancelledQuote = quote("BANK_A");
        crossBorder.cancelExpense(cancelledId);
        assertThrows(IllegalArgumentException.class, () -> phaseFour.createTuitionPlan(
                "BANK_A", cancelledQuote.sourceAccountId(), cancelledId,
                cancelled.updatedAt().toString(), cancelledQuote.quoteId()));
        assertEquals(0, planCount());
        assertEquals(0, phaseFour.sandboxTransactionCount());
    }

    @Test void cancelledPendingPlanIsAuditedAndCannotBeApprovedOrExecuted() {
        var pending = createLocked("BANK_A");
        var cancelled = phaseFour.cancel(pending.id());

        assertEquals("CANCELED", cancelled.status());
        assertTrue(phaseFour.auditEvents(pending.id()).stream()
                .anyMatch(event -> "ACTION_CANCELED".equals(event.eventType())));
        assertNull(phaseFour.approveAndExecute(pending.id()));
        assertEquals("CANCELED", phaseFour.action(pending.id()).status());
        assertNull(phaseFour.receiptForAction(pending.id()));
        assertEquals(0, phaseFour.sandboxTransactionCount());
    }

    @Test void completedPlanIsImmutableAndRepeatedApprovalReturnsSameReceipt() {
        addVerifiedExpense("Completed fee", "100.00", "COMPLETED-ONCE");
        var plan = createLocked("BANK_A");
        var first = phaseFour.approveAndExecute(plan.id());
        assertNotNull(first);
        BigDecimal balanceAfter = balance(plan.sourceAccountId());

        var repeated = phaseFour.approveAndExecute(plan.id());
        assertNotNull(repeated);
        assertEquals(first.transactionId(), repeated.transactionId());
        assertEquals(1, phaseFour.sandboxTransactionCount());
        assertEquals(4, db.queryForObject("SELECT COUNT(*) FROM sandbox_ledger_entries", Integer.class));
        assertMoney(balanceAfter, balance(plan.sourceAccountId()));
        assertThrows(IllegalArgumentException.class, () -> phaseFour.cancel(plan.id()));
        assertThrows(IllegalArgumentException.class, () -> createLocked("BANK_A"));
        assertEquals("COMPLETED", phaseFour.action(plan.id()).status());
        assertEquals(first.transactionId(), phaseFour.receiptForAction(plan.id()).transactionId());
    }

    @Test void paidBillGuardBlocksDifferentStalePlanEvenWhenItsPendingStatusIsRestored() {
        int expenseId = addVerifiedExpense("Single-payment fee", "100.00", "SINGLE-BILL");
        var old = createLocked("BANK_A");
        var replacement = createLocked("VCB");
        var receipt = phaseFour.approveAndExecute(replacement.id());
        assertNotNull(receipt);
        BigDecimal oldSourceBefore = balance(old.sourceAccountId());

        // Simulate a legacy/stale pending record; the paid-bill guard must be independent of UI invalidation.
        db.update("UPDATE action_plans SET status='AWAITING_APPROVAL' WHERE id=?", old.id());
        assertNull(phaseFour.approveAndExecute(old.id()));

        assertEquals(1, phaseFour.sandboxTransactionCount());
        assertEquals(1, db.queryForObject("""
                SELECT COUNT(*) FROM sandbox_transactions s JOIN action_plans a ON a.id=s.action_id
                WHERE a.expense_id=?
                """, Integer.class, expenseId));
        assertMoney(oldSourceBefore, balance(old.sourceAccountId()));
        assertTrue(phaseFour.auditEvents(old.id()).stream()
                .anyMatch(event -> "BILL ALREADY PAID".equals(event.reasonCode())));
    }

    @Test void concurrentIdenticalCreationProducesOnlyOnePendingPlan() throws Exception {
        var bill = crossBorder.selectedExpense();
        var quote = quote("BANK_A");
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return phaseFour.createTuitionPlan("BANK_A", quote.sourceAccountId(), bill.id(),
                        bill.updatedAt().toString(), quote.quoteId());
            });
            var second = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return phaseFour.createTuitionPlan("BANK_A", quote.sourceAccountId(), bill.id(),
                        bill.updatedAt().toString(), quote.quoteId());
            });
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            var firstPlan = first.get(15, TimeUnit.SECONDS);
            var secondPlan = second.get(15, TimeUnit.SECONDS);
            assertEquals(firstPlan.id(), secondPlan.id());
            assertEquals("AWAITING_APPROVAL", secondPlan.status());
            assertEquals(1, planCount());
            assertEquals(0, phaseFour.sandboxTransactionCount());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test void concurrentApprovalProducesOneReceiptAndOneDebit() throws Exception {
        addVerifiedExpense("Concurrent payment fee", "100.00", "CONCURRENT-PAYMENT");
        var plan = createLocked("BANK_A");
        BigDecimal sourceBefore = balance(plan.sourceAccountId());
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return phaseFour.approveAndExecute(plan.id());
            });
            var second = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return phaseFour.approveAndExecute(plan.id());
            });
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            var firstReceipt = first.get(15, TimeUnit.SECONDS);
            var secondReceipt = second.get(15, TimeUnit.SECONDS);
            assertNotNull(firstReceipt);
            assertNotNull(secondReceipt);
            assertEquals(firstReceipt.transactionId(), secondReceipt.transactionId());
            assertEquals(1, phaseFour.sandboxTransactionCount());
            assertEquals(4, db.queryForObject("SELECT COUNT(*) FROM sandbox_ledger_entries", Integer.class));
            assertMoney(sourceBefore.subtract(plan.debitAmount()), balance(plan.sourceAccountId()));
            assertEquals("COMPLETED", phaseFour.action(plan.id()).status());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
    @Test void pendingPlanWithoutTrustedSnapshotCannotBeApprovedOrExecuted() {
        var pending = createLocked("BANK_A");
        BigDecimal sourceBefore = balance(pending.sourceAccountId());
        db.update("DELETE FROM action_payment_snapshots WHERE action_id=?", pending.id());

        assertNull(phaseFour.approveAndExecute(pending.id()));

        assertEquals(0, phaseFour.sandboxTransactionCount());
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM sandbox_ledger_entries", Integer.class));
        assertMoney(sourceBefore, balance(pending.sourceAccountId()));
        assertTrue(phaseFour.auditEvents(pending.id()).stream()
                .anyMatch(event -> "PLAN SNAPSHOT MISSING".equals(event.reasonCode())));
    }
    @Test void workspaceActionHeaderKeepsHistoricalPlanAndExplicitQueryOverridesHeader() throws Exception {
        addVerifiedExpense("Historical paid fee", "100.00", "HEADER-PAID");
        var historical = createLocked("BANK_A");
        var receipt = phaseFour.approveAndExecute(historical.id());
        assertNotNull(receipt);
        var latest = phaseFour.createLowRiskPlan(new BigDecimal("1000.00"));
        assertNotEquals(historical.id(), latest.id());

        var headerModel = mvc.perform(get("/").header("X-Workspace-Action", historical.id()))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        assertEquals(historical.id(), ((PhaseFourService.ActionPlan) headerModel.get("latestAction")).id());
        assertEquals(receipt.transactionId(),
                ((PhaseFourService.Receipt) headerModel.get("latestReceipt")).transactionId());
        @SuppressWarnings("unchecked")
        var headerAudit = (List<PhaseFourService.AuditEvent>) headerModel.get("auditEvents");
        assertTrue(headerAudit.stream().allMatch(event -> historical.id().equals(event.referenceId())));

        var queryModel = mvc.perform(get("/").header("X-Workspace-Action", historical.id())
                        .param("action", latest.id()))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        assertEquals(latest.id(), ((PhaseFourService.ActionPlan) queryModel.get("latestAction")).id());
        assertNull(queryModel.get("latestReceipt"), "An explicit pending plan must override the historical receipt context");
    }

    @Test void paidBillReceiptLinkRemainsAvailableBeyondThirtyRecentPlans() throws Exception {
        int paidExpenseId = addVerifiedExpense("Long-lived paid fee", "100.00", "HISTORY-PAID");
        var completed = createLocked("BANK_A");
        var receipt = phaseFour.approveAndExecute(completed.id());
        assertNotNull(receipt);
        for (int index = 0; index < 35; index++) {
            var pending = phaseFour.createLowRiskPlan(new BigDecimal("1000.00"));
            assertEquals("AWAITING_APPROVAL", pending.status());
        }
        assertEquals(30, phaseFour.recentPlans().size());
        assertFalse(phaseFour.recentPlans().stream().anyMatch(plan -> completed.id().equals(plan.id())));

        var servicePlan = phaseFour.completedPlansByExpense().get(paidExpenseId);
        assertNotNull(servicePlan);
        assertEquals(completed.id(), servicePlan.id());
        var model = mvc.perform(get("/")).andExpect(status().isOk())
                .andReturn().getModelAndView().getModel();
        @SuppressWarnings("unchecked")
        var paidPlans = (Map<Integer, PhaseFourService.ActionPlan>) model.get("completedPlanByExpense");
        assertNotNull(paidPlans.get(paidExpenseId));
        assertEquals(completed.id(), paidPlans.get(paidExpenseId).id());
        assertEquals(receipt.transactionId(), phaseFour.receiptForAction(paidPlans.get(paidExpenseId).id()).transactionId());
        assertEquals(1, phaseFour.sandboxTransactionCount());
    }
    private int addVerifiedExpense(String title, String amount, String reference) {
        return crossBorder.addExpense("OTHER", title, CrossBorderService.SCHOOL_NAME,
                new BigDecimal(amount), "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT_NAME,
                CrossBorderService.SCHOOL_RECIPIENT_BANK, CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE,
                CrossBorderService.SCHOOL_RECIPIENT, reference, LocalDate.now().plusDays(20),
                null, null, null);
    }

    private CrossBorderService.ChannelQuote quote(String channel) {
        return crossBorder.rankedQuotes().stream().filter(candidate -> channel.equals(candidate.channelId()))
                .findFirst().orElseThrow();
    }

    private PhaseFourService.ActionPlan createLocked(String channel) {
        var bill = crossBorder.selectedExpense();
        var quote = quote(channel);
        return phaseFour.createTuitionPlan(channel, quote.sourceAccountId(), bill.id(),
                bill.updatedAt().toString(), quote.quoteId());
    }

    private int planCount() {
        return db.queryForObject("SELECT COUNT(*) FROM action_plans", Integer.class);
    }

    private BigDecimal balance(String accountId) {
        return db.queryForObject("SELECT balance FROM sandbox_accounts WHERE id=?", BigDecimal.class, accountId);
    }

    private static void assertMoney(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }
}