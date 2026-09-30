package com.example.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
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
                "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT,
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
                        .param("recipientAccount", CrossBorderService.SCHOOL_RECIPIENT)
                        .param("paymentReference", "DORM-2026-MINH")
                        .param("dueDate", LocalDate.now().plusDays(20).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/#student-finance"));

        assertEquals("dormitory-invoice.pdf", crossBorder.selectedExpense().documentName());
        String html = mvc.perform(get("/")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("Dormitory deposit"));
        assertTrue(html.contains("data-open-dialog=\"add-student-expense\""));
        assertTrue(html.contains("quote-detail-BANK_A"));
    }

    @Test void invalidExpenseAndUnsafeDocumentTypeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> crossBorder.addExpense(
                "UNKNOWN", "Unknown fee", CrossBorderService.SCHOOL_NAME, BigDecimal.ONE,
                "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT,
                "REF", LocalDate.now().plusDays(1), null, null, null));
        assertThrows(IllegalArgumentException.class, () -> crossBorder.addExpense(
                "OTHER", "Unknown fee", CrossBorderService.SCHOOL_NAME, BigDecimal.ONE,
                "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT,
                "REF", LocalDate.now().plusDays(1),
                "unsafe.exe", "application/octet-stream", 10L));
    }

    @Test void selectedUsExpenseDrivesCorridorQuotesPlanAndSandboxCurrency() {
        crossBorder.addExpense("TUITION", "Fall tuition", CrossBorderService.US_SCHOOL_NAME,
                new BigDecimal("2500.00"), "United States", "USD",
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
}
