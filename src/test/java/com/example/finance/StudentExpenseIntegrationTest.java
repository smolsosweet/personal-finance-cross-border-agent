package com.example.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:student_expense_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class StudentExpenseIntegrationTest {
    @Autowired CrossBorderService crossBorder;
    @Autowired PhaseFourService phaseFour;
    @Autowired DemoDataService demoData;
    @Autowired MockMvc mvc;

    @BeforeEach void reset() {
        demoData.resetAll();
    }

    @Test void manualExpenseBecomesSelectedAndRecalculatesQuotes() {
        int id = crossBorder.addExpense("DORMITORY", "Dormitory deposit",
                CrossBorderService.SCHOOL_NAME, new BigDecimal("2500.00"),
                "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT_NAME, CrossBorderService.SCHOOL_RECIPIENT_BANK,
                CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE, CrossBorderService.SCHOOL_RECIPIENT,
                "DORM-2026-MINH", LocalDate.now().plusDays(20),
                null, null, null);

        assertEquals(id, crossBorder.selectedExpense().id());
        assertEquals("Dormitory deposit", crossBorder.selectedExpense().title());
        assertEquals(0, new BigDecimal("2500.00").compareTo(crossBorder.bill().amount()));
        assertTrue(crossBorder.verifyRecipient().verified());
        assertTrue(crossBorder.rankedQuotes().stream().allMatch(quote ->
                new BigDecimal("2500.00").compareTo(quote.expectedReceived()) == 0));

        var plan = phaseFour.createTuitionPlan("BANK_A");
        assertEquals(0, new BigDecimal("2500.00").compareTo(plan.destinationAmount()));
        assertTrue(plan.purpose().contains("Dormitory deposit"));

        crossBorder.selectExpense(1);
        assertEquals(1, crossBorder.selectedExpense().id());
        assertEquals(0, CrossBorderService.TUITION_AMOUNT.compareTo(crossBorder.bill().amount()));
        assertFalse(phaseFour.matchesCurrentStudentSelection(plan));
    }

    @Test void selectingBillKeepsEqualDeadlineExpensesInTheirOriginalOrder() {
        LocalDate deadline = crossBorder.selectedExpense().dueDate();
        int second = crossBorder.addExpense("INSURANCE", "Campus stable insurance",
                CrossBorderService.SCHOOL_NAME, new BigDecimal("1200.00"), "China", "CNY",
                CrossBorderService.SCHOOL_RECIPIENT_NAME, CrossBorderService.SCHOOL_RECIPIENT_BANK,
                CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE, CrossBorderService.SCHOOL_RECIPIENT,
                "STABLE-SECOND", deadline, null, null, null);
        int third = crossBorder.addExpense("DORMITORY", "Campus stable dormitory",
                CrossBorderService.SCHOOL_NAME, new BigDecimal("1200.00"), "China", "CNY",
                CrossBorderService.SCHOOL_RECIPIENT_NAME, CrossBorderService.SCHOOL_RECIPIENT_BANK,
                CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE, CrossBorderService.SCHOOL_RECIPIENT,
                "STABLE-THIRD", deadline, null, null, null);

        List<Integer> initialOrder = crossBorder.expenses().stream()
                .map(CrossBorderService.StudentExpense::id).toList();
        assertEquals(List.of(1, second, third), initialOrder,
                "Equal deadlines must use a stable order independent of the selected bill");
        for (int selected : List.of(second, 1, third)) {
            crossBorder.selectExpense(selected);
            assertEquals(initialOrder, crossBorder.expenses().stream()
                    .map(CrossBorderService.StudentExpense::id).toList());
            assertEquals(selected, crossBorder.selectedExpense().id());
            assertEquals(1, crossBorder.expenses().stream()
                    .filter(CrossBorderService.StudentExpense::selected).count());
        }
    }

    @Test void pdfUploadStoresSafeMetadataAndRendersProductionUi() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document", "dormitory-invoice.pdf", "application/pdf", "synthetic document".getBytes());

        mvc.perform(multipart("/student/expenses")
                        .file(document)
                        .param("expenseType", "DORMITORY")
                        .param("title", "Dormitory deposit")
                        .param("institution", CrossBorderService.SCHOOL_NAME)
                        .param("amount", "2500.00")
                        .param("destinationCountry", "China")
                        .param("currency", "CNY")
                        .param("recipientName", CrossBorderService.SCHOOL_RECIPIENT_NAME)
                        .param("recipientBankName", CrossBorderService.SCHOOL_RECIPIENT_BANK)
                        .param("recipientBankCode", CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE)
                        .param("recipientAccount", CrossBorderService.SCHOOL_RECIPIENT)
                        .param("paymentReference", "DORM-2026-MINH")
                        .param("dueDate", LocalDate.now().plusDays(20).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/#student-finance"));

        assertNull(crossBorder.selectedExpenseOrNull());
        assertEquals("dormitory-invoice.pdf", expense(2).documentName());
        String html = mvc.perform(get("/")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("Dormitory deposit"));
        assertTrue(html.contains("data-open-dialog=\"add-student-expense\""));
        assertTrue(html.contains("data-testid=\"student-no-selection\""));
        assertFalse(html.contains("id=\"quote-detail-BANK_A\""));
        mvc.perform(post("/student/expenses/select").param("id", "2"))
                .andExpect(status().is3xxRedirection());
        assertEquals(2, crossBorder.selectedExpense().id());
        html = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("id=\"quote-detail-BANK_A\""));
    }

    @Test void invalidExpenseAndUnsafeDocumentTypeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> crossBorder.addExpense(
                "UNKNOWN", "Unknown fee", CrossBorderService.SCHOOL_NAME, BigDecimal.ONE,
                "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT_NAME, CrossBorderService.SCHOOL_RECIPIENT_BANK,
                CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE, CrossBorderService.SCHOOL_RECIPIENT,
                "REF", LocalDate.now().plusDays(1), null, null, null));
        assertThrows(IllegalArgumentException.class, () -> crossBorder.addExpense(
                "OTHER", "Unknown fee", CrossBorderService.SCHOOL_NAME, BigDecimal.ONE,
                "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT_NAME, CrossBorderService.SCHOOL_RECIPIENT_BANK,
                CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE, CrossBorderService.SCHOOL_RECIPIENT,
                "REF", LocalDate.now().plusDays(1),
                "unsafe.exe", "application/octet-stream", 10L));
    }

    @Test void selectedUsExpenseDrivesCorridorQuotesPlanAndSandboxCurrency() {
        crossBorder.addExpense("TUITION", "Fall tuition", CrossBorderService.US_SCHOOL_NAME,
                new BigDecimal("2500.00"), "United States", "USD", CrossBorderService.US_SCHOOL_RECIPIENT_NAME,
                CrossBorderService.US_SCHOOL_RECIPIENT_BANK, CrossBorderService.US_SCHOOL_RECIPIENT_BANK_CODE,
                CrossBorderService.US_SCHOOL_RECIPIENT, "PDC-2026-MINH", LocalDate.now().plusDays(20),
                null, null, null);

        assertEquals("United States", crossBorder.profile().destinationCountry());
        assertEquals("USD", crossBorder.profile().destinationCurrency());
        assertEquals("USD", crossBorder.bill().currency());
        assertTrue(crossBorder.verifyRecipient().verified());
        assertEquals(3, crossBorder.rankedQuotes().stream().filter(CrossBorderService.ChannelQuote::eligible).count());
        assertTrue(crossBorder.rankedQuotes().stream()
                .filter(quote -> quote.channelId().equals("ALIPAY") || quote.channelId().equals("MOMO"))
                .noneMatch(CrossBorderService.ChannelQuote::eligible));

        var plan = phaseFour.createTuitionPlan("BANK_A");
        assertEquals("USD", plan.destinationCurrency());
        assertEquals("APPROVAL", plan.requiredPermission());
        var receipt = phaseFour.approveAndExecute(plan.id());
        assertEquals("USD", receipt.destinationCurrency());
        assertEquals(0, new BigDecimal("2500.00").compareTo(receipt.cnyCredit()));
    }

    @Test void mismatchedBeneficiaryIsStoredForReviewButCannotBeSelected() {
        int id = crossBorder.addExpense("OTHER", "Unverified provider fee",
                "Unknown Education Provider", new BigDecimal("300.00"), "China", "CNY",
                "Unknown Recipient", "Unknown Bank", "UNKNOWNCODE", "UNKNOWN-ACCOUNT",
                "UNKNOWN-REF", LocalDate.now().plusDays(10), null, null, null);

        var stored = expense(id);
        assertEquals("MISMATCH", stored.verificationStatus());
        assertFalse(stored.selected());
        assertEquals(1, crossBorder.selectedExpense().id());
        assertThrows(IllegalArgumentException.class, () -> crossBorder.selectExpense(id));
    }
    @Test void editingBillInvalidatesItsPendingPlanAndApprovalPath() throws Exception {
        int id = crossBorder.addExpense("DORMITORY", "Dormitory deposit",
                CrossBorderService.SCHOOL_NAME, new BigDecimal("2500.00"), "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT_NAME,
                CrossBorderService.SCHOOL_RECIPIENT_BANK, CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE,
                CrossBorderService.SCHOOL_RECIPIENT, "DORM-EDIT-1", LocalDate.now().plusDays(20),
                null, null, null);
        var plan = phaseFour.createTuitionPlan("BANK_A");
        assertEquals(id, plan.expenseId());
        assertEquals("AWAITING_APPROVAL", plan.status());

        crossBorder.updateExpense(id, "DORMITORY", "Updated dormitory deposit",
                CrossBorderService.SCHOOL_NAME, new BigDecimal("2600.00"), "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT_NAME,
                CrossBorderService.SCHOOL_RECIPIENT_BANK, CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE,
                CrossBorderService.SCHOOL_RECIPIENT, "DORM-EDIT-2", LocalDate.now().plusDays(21));

        assertEquals("INVALIDATED", phaseFour.action(plan.id()).status());
        assertNull(phaseFour.approveAndExecute(plan.id()));
        assertEquals("INVALIDATED", phaseFour.action(plan.id()).status());
        assertEquals(0, phaseFour.sandboxTransactionCount());
        assertEquals("Updated dormitory deposit", crossBorder.selectedExpense().title());
        var model = mvc.perform(get("/").param("action", plan.id())).andExpect(status().isOk())
                .andReturn().getModelAndView().getModel();
        var visiblePlan = (PhaseFourService.ActionPlan) model.get("latestAction");
        assertEquals(plan.id(), visiblePlan.id());
        assertEquals("INVALIDATED", visiblePlan.status());
        assertNull(model.get("latestReceipt"));
        assertEquals("ACTION INVALIDATED", ((PhaseFourService.PolicyDecision) model.get("paymentDecision")).reasonCode());
    }

    @Test void activeBillCanBeArchivedRestoredAndCancelled() {
        int id = crossBorder.addExpense("INSURANCE", "Student insurance",
                CrossBorderService.SCHOOL_NAME, new BigDecimal("500.00"), "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT_NAME,
                CrossBorderService.SCHOOL_RECIPIENT_BANK, CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE,
                CrossBorderService.SCHOOL_RECIPIENT, "INS-1", LocalDate.now().plusDays(25),
                null, null, null);
        var plan = phaseFour.createTuitionPlan("BANK_A");

        crossBorder.archiveExpense(id);
        assertEquals("ARCHIVED", expense(id).lifecycleStatus());
        assertEquals("INVALIDATED", phaseFour.action(plan.id()).status());
        assertEquals(1, crossBorder.selectedExpense().id());

        crossBorder.restoreExpense(id);
        assertEquals("ACTIVE", expense(id).lifecycleStatus());
        crossBorder.cancelExpense(id);
        assertEquals("CANCELLED", expense(id).lifecycleStatus());
        assertFalse(expense(id).selected());
    }

    @Test void executedBillCannotBeEditedOrCancelledButCanBeArchived() {
        int id = crossBorder.addExpense("TUITION", "Paid tuition",
                CrossBorderService.US_SCHOOL_NAME, new BigDecimal("1000.00"), "United States", "USD", CrossBorderService.US_SCHOOL_RECIPIENT_NAME,
                CrossBorderService.US_SCHOOL_RECIPIENT_BANK, CrossBorderService.US_SCHOOL_RECIPIENT_BANK_CODE,
                CrossBorderService.US_SCHOOL_RECIPIENT, "PAID-1", LocalDate.now().plusDays(30),
                null, null, null);
        var plan = phaseFour.createTuitionPlan("BANK_A");
        var receipt = phaseFour.approveAndExecute(plan.id());
        assertEquals("COMPLETED", receipt.status());
        assertTrue(expense(id).executed());

        assertThrows(IllegalArgumentException.class, () -> crossBorder.updateExpense(id, "TUITION",
                "Changed paid tuition", CrossBorderService.US_SCHOOL_NAME, new BigDecimal("1100.00"),
                "United States", "USD", CrossBorderService.US_SCHOOL_RECIPIENT_NAME,
                CrossBorderService.US_SCHOOL_RECIPIENT_BANK, CrossBorderService.US_SCHOOL_RECIPIENT_BANK_CODE,
                CrossBorderService.US_SCHOOL_RECIPIENT,
                "PAID-CHANGED", LocalDate.now().plusDays(31)));
        assertThrows(IllegalArgumentException.class, () -> crossBorder.cancelExpense(id));

        crossBorder.archiveExpense(id);
        assertEquals("ARCHIVED", expense(id).lifecycleStatus());
        assertTrue(expense(id).executed());
        assertEquals(1, phaseFour.sandboxTransactionCount());
    }

    private CrossBorderService.StudentExpense expense(int id) {
        return crossBorder.expenses().stream().filter(item -> item.id() == id).findFirst().orElseThrow();
    }

    @Test void deselectingTheOnlyBillRendersStepOneAndDoesNotInvalidateItsLockedPlan() throws Exception {
        var plan = phaseFour.createTuitionPlan("BANK_A");
        mvc.perform(post("/student/expenses/select").param("id", "1").param("deselect", "true"))
                .andExpect(status().is3xxRedirection());
        assertNull(crossBorder.selectedExpenseOrNull());
        assertTrue(crossBorder.rankedQuotes().isEmpty());
        assertTrue(crossBorder.tuitionInsight().isEmpty());
        assertThrows(IllegalStateException.class, crossBorder::bill);
        assertFalse(phaseFour.matchesCurrentStudentSelection(plan));
        var model = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        assertEquals(1, model.get("studentWorkflowStage"));
        assertEquals("AWAITING_APPROVAL", phaseFour.action(plan.id()).status());
        assertEquals(0, phaseFour.sandboxTransactionCount());
        var receipt = phaseFour.approveAndExecute(plan.id());
        assertTrue(receipt != null && receipt.actionId().equals(plan.id()));
        assertEquals(1, phaseFour.sandboxTransactionCount());
    }

    @Test void savingAnotherBillForReviewDoesNotReplaceTheQuoteOfAnExistingPlan() {
        var plan = phaseFour.createTuitionPlan("BANK_A");
        crossBorder.addExpenseForReview("TUITION", "New USD tuition", CrossBorderService.US_SCHOOL_NAME,
                new BigDecimal("1000"), "United States", "USD", CrossBorderService.US_SCHOOL_RECIPIENT_NAME,
                CrossBorderService.US_SCHOOL_RECIPIENT_BANK, CrossBorderService.US_SCHOOL_RECIPIENT_BANK_CODE,
                CrossBorderService.US_SCHOOL_RECIPIENT, "REVIEW-USD", LocalDate.now().plusDays(30), null, null, null);
        assertNull(crossBorder.selectedExpenseOrNull());
        var sameQuote = crossBorder.rankedQuotesForExpense(1).stream()
                .filter(q -> q.channelId().equals("BANK_A")).findFirst().orElseThrow();
        assertEquals(plan.quoteId(), sameQuote.quoteId());
        assertEquals("AWAITING_APPROVAL", phaseFour.action(plan.id()).status());
        assertEquals(0, phaseFour.sandboxTransactionCount());
        var receipt = phaseFour.approveAndExecute(plan.id());
        assertTrue(receipt != null && receipt.actionId().equals(plan.id()));
        assertEquals("CNY", receipt.destinationCurrency());
        assertFalse(expense(2).executed());
    }

    @Test void staleDeselectCannotClearAnotherBillAndProgressCannotBorrowAnotherBillsReceipt() throws Exception {
        var paid = phaseFour.createTuitionPlan("BANK_A");
        phaseFour.approveAndExecute(paid.id());
        int second = crossBorder.addExpense("TUITION", "Second tuition", CrossBorderService.SCHOOL_NAME,
                new BigDecimal("1000"), "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT_NAME,
                CrossBorderService.SCHOOL_RECIPIENT_BANK, CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE,
                CrossBorderService.SCHOOL_RECIPIENT, "SECOND", LocalDate.now().plusDays(30), null, null, null);
        assertFalse(crossBorder.clearExpenseSelection(1));
        var stale = mvc.perform(post("/student/expenses/select").param("id", "1").param("deselect", "true"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertEquals("The selected bill changed. Your current selection was kept; review it before continuing.",
                stale.getFlashMap().get("message"));
        assertEquals(second, crossBorder.selectedExpense().id());
        var model = mvc.perform(get("/").param("action", paid.id())).andExpect(status().isOk())
                .andReturn().getModelAndView().getModel();
        assertEquals(3, model.get("studentWorkflowStage"));
        assertNull(model.get("studentWorkflowPlan"));
        var pending = phaseFour.createTuitionPlan("BANK_A");
        model = mvc.perform(get("/").param("action", paid.id())).andExpect(status().isOk())
                .andReturn().getModelAndView().getModel();
        assertEquals(4, model.get("studentWorkflowStage"));
        assertEquals(pending.id(), ((PhaseFourService.ActionPlan) model.get("studentWorkflowPlan")).id());
        phaseFour.cancel(pending.id());
        model = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        assertEquals(3, model.get("studentWorkflowStage"));
        assertEquals(1, phaseFour.sandboxTransactionCount());
    }
}
