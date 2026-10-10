package com.example.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Presentation lifecycle regressions; synthetic, isolated database and no model calls. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:ui_business_state;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "finbridge.llm.enabled=false"})
@AutoConfigureMockMvc
class UiBusinessStateIntegrationTest {
    @Autowired DemoDataService demo;
    @Autowired CrossBorderService bills;
    @Autowired PhaseFourService payments;
    @Autowired SessionConversationService chat;
    @Autowired MockMvc mvc;
    @Autowired org.springframework.jdbc.core.JdbcTemplate db;

    @BeforeEach void reset(){demo.resetAll();}
    int anotherBill(){return bills.addExpense("TUITION","Other tuition",CrossBorderService.SCHOOL_NAME,
            new BigDecimal("1000"),"China","CNY",CrossBorderService.SCHOOL_RECIPIENT_NAME,
            CrossBorderService.SCHOOL_RECIPIENT_BANK,CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE,
            CrossBorderService.SCHOOL_RECIPIENT,"OTHER-STATE",LocalDate.now().plusDays(30),null,null,null);}
    Map<String,Object> page() throws Exception{return mvc.perform(get("/")).andReturn().getModelAndView().getModel();}
    @SuppressWarnings("unchecked") List<Map<String,Object>> education() throws Exception {
        return ((List<Map<String,Object>>)page().get("insights")).stream()
                .filter(i->i.getOrDefault("notificationKey","").toString().startsWith("education-bill-")).toList();
    }
    void noActionFor(int id) throws Exception {
        assertFalse(education().stream().anyMatch(i->("education-bill-"+id).equals(i.get("notificationKey"))
                && "ACTION".equals(i.get("notificationKind"))));
    }
    @Test void completedPaymentHasOnlyAnInformationalReceiptNotification() throws Exception {
        var plan=payments.createTuitionPlan("BANK_A");var receipt=payments.approveAndExecute(plan.id());
        var item=education().getFirst();assertEquals("UPDATE",item.get("notificationKind"));
        assertEquals("Payment completed",item.get("actionState"));assertTrue(item.get("evidence").toString().contains(receipt.transactionId()));
        noActionFor(1);
    }
    @Test void creatingBillThroughTheUiStartsAtSelectionUntilExplicitlySelected() throws Exception {
        mvc.perform(post("/student/expenses").param("expenseType","TUITION").param("title","New tuition")
                .param("institution",CrossBorderService.SCHOOL_NAME).param("amount","1000")
                .param("destinationCountry","China").param("currency","CNY")
                .param("recipientName",CrossBorderService.SCHOOL_RECIPIENT_NAME)
                .param("recipientBankName",CrossBorderService.SCHOOL_RECIPIENT_BANK)
                .param("recipientBankCode",CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE)
                .param("recipientAccount",CrossBorderService.SCHOOL_RECIPIENT).param("paymentReference","NEW-STATE")
                .param("dueDate",LocalDate.now().plusDays(30).toString()));
        assertNull(bills.selectedExpenseOrNull());assertEquals(1,page().get("studentWorkflowStage"));
        mvc.perform(post("/student/expenses/select").param("id","2"));
        assertEquals(2,bills.selectedExpense().id());assertEquals(3,page().get("studentWorkflowStage"));
    }
    @Test void cancellingTheOnlySelectedBillReturnsToEmptySelection() throws Exception {
        bills.cancelExpense(1);assertNull(bills.selectedExpenseOrNull());
        assertEquals("CANCELLED",bills.expense(1).lifecycleStatus());assertEquals(1,page().get("studentWorkflowStage"));
        noActionFor(1);
    }
    @Test void archivingTheOnlySelectedBillReturnsToEmptySelection() throws Exception {
        bills.archiveExpense(1);assertNull(bills.selectedExpenseOrNull());
        assertEquals("ARCHIVED",bills.expense(1).lifecycleStatus());assertEquals(1,page().get("studentWorkflowStage"));
        noActionFor(1);
    }
    @Test void cancelAndArchiveUseAnotherValidSelectionAndRemoveOnlyTheRemovedBillsReminder() throws Exception {
        int id=anotherBill();bills.cancelExpense(id);assertEquals(1,bills.selectedExpense().id());noActionFor(id);
        id=anotherBill();bills.archiveExpense(id);assertEquals(1,bills.selectedExpense().id());noActionFor(id);
    }
    @Test void anotherSessionsBillChangeHidesHistoricalShortcutWithoutDeletingHistory() {
        var paid=payments.createTuitionPlan("BANK_A");payments.approveAndExecute(paid.id());
        var session=new MockHttpSession();var view=chat.view(session,bills.selectedExpense(),paid);
        chat.enter(session,view.studentToken(),false,false);assertEquals(paid.id(),chat.assistantPlanId(session));
        int messages=chat.view(session,bills.selectedExpense(),paid).messages().size();
        anotherBill();assertNull(chat.assistantPlanId(session),"A teammate changed the active bill; old receipt shortcut must not imply current context");
        assertEquals(messages,chat.view(session,bills.selectedExpense(),paid).messages().size());
        assertNotNull(payments.receiptForAction(paid.id()));assertEquals(1,payments.sandboxTransactionCount());
    }
    @Test void emergencyStopAndResetReplayRecomputeFromAuthoritativeState() throws Exception {
        var plan=payments.createTuitionPlan("BANK_A");payments.emergencyStop();
        var blocked=education().getFirst();assertEquals("ACTION",blocked.get("notificationKind"));
        assertEquals("Review blocked plan",blocked.get("actionState"));assertEquals("AWAITING_APPROVAL",payments.action(plan.id()).status());
        demo.resetAll();assertEquals("ACTIVE",payments.policy().state());assertEquals(0,payments.sandboxTransactionCount());
        assertTrue(education().stream().noneMatch(i->i.get("message").toString().contains(plan.id())));
        var replay=payments.createTuitionPlan("BANK_A");payments.approveAndExecute(replay.id());
        assertEquals("Payment completed",education().getFirst().get("actionState"));
    }
    @Test void notificationRefreshIsReadOnlyAndDoesNotCreateChatHistory() throws Exception {
        payments.createTuitionPlan("BANK_A");var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        String html=mvc.perform(get("/notifications").session(new MockHttpSession())).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("id=\"notification-center\""));assertFalse(html.contains("class=\"shell\""));
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
    }
}
