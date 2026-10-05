package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.microsoft.playwright.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Real local app and Chrome; intent classifier mocked, isolated H2 only. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
    "server.port=8131","spring.datasource.url=jdbc:h2:mem:global_browser;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.demo-tools-enabled=false"})
class GlobalAssistantPlaywrightTest {
    @Autowired DemoDataService demo; @Autowired PhaseFourService payments; @Autowired CrossBorderService border; @Autowired JdbcTemplate db;
    @MockitoBean LlmIntentClient model;
    Playwright pw; Browser browser; Page page; PhaseFourService.ActionPlan plan;
    final List<String> errors=new ArrayList<>();
    final List<String> tabs=List.of("dashboard","transactions","student","agent");
    @BeforeEach void setup(){
        reset(model);when(model.enabled()).thenReturn(true);demo.resetAll();plan=payments.createTuitionPlan("BANK_A");
        pw=Playwright.create();browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
        page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1440,1000));page.setDefaultTimeout(15000);
        page.onPageError(errors::add);page.onDialog(Dialog::accept);page.navigate("http://localhost:8131");
        page.getByTestId("assistant-launcher").click();
    }
    @AfterEach void close(){if(browser!=null)browser.close();if(pw!=null)pw.close();assertTrue(errors.isEmpty(),errors.toString());}
    void stub(LlmIntent.Intent intent){doReturn(new LlmIntent(intent,LlmIntent.ChannelPreference.NONE,new BigDecimal("0.95"),LlmIntent.ClarificationCode.NONE)).when(model).classify(anyString());}
    void idle(){page.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')");}
    String send(String text,LlmIntent.Intent intent,String language){
        stub(intent);page.getByTestId("assistant-language-"+language).click();
        page.getByTestId("assistant-conversation-input").fill(text);page.getByTestId("assistant-send-message").click();idle();
        return reply().locator("[data-response-raw]").textContent();
    }
    Locator reply(){return page.getByTestId("assistant-replies").locator(".message.assistant").last();}
    String financialText(String value){return value.replaceAll("(?m)^(Observed at:|Quan sát lúc:).*$","").trim();}
    @Test void fixedCorpusHasSameResultFromEveryTabWithoutContextButtonsOrFinancialMutations(){
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        for(var row:GlobalAssistantCorpus.cases()){
            String first=null;
            for(String tab:tabs){
                page.getByTestId("tab-"+tab).click();
                String actual=send(row.text(),row.intent(),row.language());
                assertTrue(actual.contains(row.evidence()),tab+" "+row.text()+" => "+actual);
                String result=financialText(actual);
                if(first==null)first=result;else assertEquals(first,result,tab+" "+row.text());
                assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
            }
        }
        assertEquals("AWAITING_APPROVAL",payments.action(plan.id()).status());assertNull(payments.latestReceipt());
    }
    @Test void historicalAndCurrentBalancesRemainDifferentAcrossAllTabsAfterActualPayment(){
        var receipt=payments.approveAndExecute(plan.id());assertNotNull(receipt);
        var subsequent=payments.createLowRiskPlan(new BigDecimal("500000"));assertNotNull(payments.approveAndExecute(subsequent.id()));
        db.update("UPDATE fx_quotes SET expires_at=?",LocalDateTime.now().minusMinutes(1));
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        for(String tab:tabs){
            page.getByTestId("tab-"+tab).click();
            assertTrue(send("Số dư ngay sau giao dịch "+receipt.transactionId(),LlmIntent.Intent.EXPLAIN_RECEIPT_BALANCE,"vi").contains("29239200.00"));
            assertThat(reply().getByTestId("response-estimate")).hasCount(0);
            assertTrue(send("Hiện Bank A Everyday có bao nhiêu?",LlmIntent.Intent.EXPLAIN_CURRENT_BALANCE,"vi").contains("28739200.00"));
            assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
        }
        assertEquals(receipt,payments.receiptForAction(plan.id()));
    }
    @Test void pendingAccountChoiceIsVisibleAndBoundOnlyToThisSession(){
        db.update("INSERT INTO sandbox_accounts VALUES ('BANK_A_EXTRA','Bank A Savings','VND',50000000)");
        db.update("INSERT INTO payment_source_accounts SELECT 'BANK_A_EXTRA','Bank A','Savings','•••• 7715','CONNECTED','VERIFIED',FALSE,FALSE,9 FROM payment_source_accounts WHERE account_id='PAYER_VND'");
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        send("Hiện Bank A còn bao nhiêu?",LlmIntent.Intent.EXPLAIN_CURRENT_BALANCE,"vi");
        assertThat(page.getByTestId("assistant-context-state")).containsText("Số dư tài khoản hiện tại");
        assertThat(page.getByTestId("assistant-pending")).containsText("Chọn tài khoản");
        assertThat(page.getByTestId("assistant-context-choice")).hasCount(2);
        page.getByTestId("assistant-context-choice").filter(new Locator.FilterOptions().setHasText("7715")).click();idle();
        assertTrue(send("Bây giờ còn bao nhiêu?",LlmIntent.Intent.EXPLAIN_CURRENT_BALANCE,"vi").contains("50000000.00"));
        try(BrowserContext other=browser.newContext()){
            Page fresh=other.newPage();fresh.navigate("http://localhost:8131");fresh.getByTestId("assistant-launcher").click();
            assertThat(fresh.getByTestId("assistant-replies")).not().containsText("Bank A Savings");
        }
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
    }
    @Test void everyPlanStateHasAnAccurateBilingualBanner(){
        var states=Map.of("AWAITING_APPROVAL","explicit approval","COMPLETED","Payment completed","INVALIDATED","Plan invalidated",
                "BLOCKED","Plan blocked","CANCELED","Plan canceled","APPROVED","Reload the authoritative plan state");
        for(var entry:states.entrySet()){
            // Presentation-only state fixtures; execution is exercised by separate unchanged Sandbox tests.
            db.update("UPDATE action_plans SET status=? WHERE id=?",entry.getKey(),plan.id());
            page.navigate("http://localhost:8131/?action="+plan.id()+"#agent-workspace");
            page.reload();
            if(!page.getByTestId("assistant-panel").isVisible())page.getByTestId("assistant-launcher").click();
            page.getByTestId("assistant-language-en").click();
            assertThat(page.getByTestId("assistant-plan-state")).containsText(entry.getValue());
            if(!entry.getKey().equals("AWAITING_APPROVAL"))assertThat(page.getByTestId("assistant-plan-state")).not().containsText("requires explicit approval");
            page.getByTestId("assistant-language-vi").click();
            assertThat(page.getByTestId("assistant-plan-state")).not().containsText(entry.getValue());
        }
    }
    @Test void draftBFromChatReviewsAndPaysBOnlyWhileAStaysSelected(){
        int a=border.selectedExpense().id(),b=GlobalAssistantFixtures.secondTuition(border);
        String source=payments.selectedPaymentSource().accountId();
        page.getByTestId("tab-transactions").click();
        String answer=send("Prepare tuition draft TUITION-B-DEMO using Vietcombank",LlmIntent.Intent.CREATE_TUITION_PLAN,"en");
        var draft=payments.latestAction();assertTrue(answer.contains(draft.id()),answer);
        assertEquals(b,draft.expenseId());assertEquals(0,payments.sandboxTransactionCount());
        assertEquals(a,border.selectedExpense().id());assertEquals(source,payments.selectedPaymentSource().accountId());
        page.getByTestId("assistant-review-plan").click();page.getByTestId("tab-agent").click();
        assertThat(page.getByTestId("assistant-plan-state")).containsText("requires explicit approval");
        assertThat(page.getByTestId("assistant-panel")).containsText("TUITION-B-DEMO");
        if(page.getByTestId("assistant-panel").isVisible())page.getByTestId("assistant-close").click();
        page.getByTestId("approve-action").click();idle();
        assertEquals(1,payments.sandboxTransactionCount());assertNotNull(payments.receiptForAction(draft.id()));
        assertFalse(border.expense(a).executed());assertTrue(border.expense(b).executed());
        assertEquals(a,border.selectedExpense().id());assertEquals(source,payments.selectedPaymentSource().accountId());
        page.reload();assertThat(page.getByTestId("latest-receipt")).hasAttribute("data-transaction-id",payments.receiptForAction(draft.id()).transactionId());
    }
}
