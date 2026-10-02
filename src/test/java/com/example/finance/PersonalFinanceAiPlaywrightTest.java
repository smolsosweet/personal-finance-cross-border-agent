package com.example.finance;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.microsoft.playwright.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT, properties={
    "server.port=8092",
    "spring.datasource.url=jdbc:h2:mem:personal_ai_browser;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
class PersonalFinanceAiPlaywrightTest {
    @Autowired DemoDataService demo;
    @Autowired PhaseFourService payments;
    @Autowired JdbcTemplate db;
    @MockitoBean LlmIntentClient llm;
    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeEach void setup() {
        org.mockito.Mockito.reset(llm);
        when(llm.enabled()).thenReturn(true);
        demo.resetAll();
        playwright=Playwright.create();
        browser=playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
        page=browser.newPage();
        page.setDefaultTimeout(10000);
        page.onDialog(Dialog::accept);
        page.navigate("http://localhost:8092");
    }
    @AfterEach void close() { if(browser!=null)browser.close(); if(playwright!=null)playwright.close(); }

    @Test void waitingDuplicateGuardEvidenceAndReadonlyStateAreVisible() {
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        AtomicInteger calls=new AtomicInteger();
        when(llm.classify(anyString())).thenAnswer(call -> {
            calls.incrementAndGet(); Thread.sleep(1500);
            return intent(LlmIntent.Intent.EXPLAIN_BUDGET_STATUS);
        });
        page.locator("[data-language='vi']").click();
        page.getByTestId("finance-conversation-input").fill("Ngân sách tháng này còn bao nhiêu?");
        page.getByTestId("finance-send-message").click();
        assertThat(page.getByTestId("finance-chat").locator("[data-chat-processing]")).isVisible();
        assertThat(page.getByTestId("finance-send-message")).isDisabled();
        assertThat(page.getByTestId("finance-conversation-input")).isDisabled();
        page.evaluate("""
            () => { const f=document.querySelector('[data-testid="finance-chat"] form');
              f.requestSubmit(); f.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));
              document.querySelector('[data-testid="finance-conversation-input"]').dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true})); }
            """);
        idle();
        assertThat(page.getByTestId("finance-send-message")).isEnabled();
        assertThat(page.getByTestId("finance-chat").locator("[data-chat-processing]")).isHidden();
        assertThat(page.getByTestId("finance-replies")).containsText("Nguồn: ngân sách cấu hình");
        assertThat(page.getByTestId("finance-replies")).containsText("VND");
        assertEquals(1,calls.get());
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
    }

    @Test void networkErrorClientTimeoutAndBackendFailureRestoreControlsWithoutRetry() {
        when(llm.classify(anyString())).thenReturn(intent(LlmIntent.Intent.EXPLAIN_SPENDING_SUMMARY));
        page.route("**/agent/message", route -> route.abort());
        send("Show spending");
        idle();
        assertThat(page.getByTestId("finance-send-message")).isEnabled();
        assertThat(page.locator(".request-error")).containsText("No automatic retry");
        verify(llm,never()).classify(anyString());
        page.unroute("**/agent/message");
        // An unresolved intercepted request exercises the configured client timeout recovery.
        page.route("**/agent/message", route -> {});
        page.getByTestId("finance-chat").locator("form").evaluate("f => f.dataset.chatTimeout='250'");
        send("Show spending");
        idle();
        assertThat(page.getByTestId("finance-send-message")).isEnabled();
        page.unroute("**/agent/message");
        doThrow(new LlmIntentException("provider timeout")).when(llm).classify(anyString());
        send("Show spending");
        idle();
        assertThat(page.getByTestId("finance-replies")).containsText("AI is temporarily unavailable or timed out");
        assertThat(page.getByTestId("finance-send-message")).isEnabled();
        doReturn(intent(LlmIntent.Intent.EXPLAIN_SPENDING_SUMMARY)).when(llm).classify(anyString());
        send("Show spending");
        idle();
        assertThat(page.getByTestId("finance-replies")).containsText("Evidence: recorded transactions");
        assertNull(payments.latestAction());
    }

    @Test void resetDiscardsLateChatAndExistingTuitionPlanStillNeedsApproval() {
        AtomicInteger calls=new AtomicInteger();
        when(llm.classify(anyString())).thenAnswer(call -> {
            calls.incrementAndGet(); Thread.sleep(1800);
            return intent(LlmIntent.Intent.CREATE_TUITION_PLAN);
        });
        send("Prepare tuition draft");
        page.waitForCondition(() -> calls.get()==1);
        page.getByTestId("reset-demo").click();
        idle();
        page.waitForTimeout(2200);
        assertNull(payments.latestAction());
        assertThat(page.getByTestId("finance-replies")).not().containsText("Prepared tuition-payment plan");
        doReturn(intent(LlmIntent.Intent.CREATE_TUITION_PLAN)).when(llm).classify(anyString());
        send("Prepare tuition draft");
        idle();
        assertEquals("AWAITING_APPROVAL",payments.latestAction().status());
        assertEquals(0,payments.sandboxTransactionCount());
        page.getByTestId("tab-agent").click();
        assertThat(page.getByTestId("latest-action")).hasAttribute("data-status","AWAITING_APPROVAL");
        assertThat(page.getByTestId("latest-receipt")).hasCount(0);
        assertThat(page.getByTestId("approve-action")).isVisible();
    }

    private void send(String question) {
        page.getByTestId("finance-conversation-input").fill(question);
        page.getByTestId("finance-send-message").click();
    }
    private void idle() { page.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')"); }
    private LlmIntent intent(LlmIntent.Intent value) {
        return new LlmIntent(value,LlmIntent.ChannelPreference.NONE,new BigDecimal("0.95"),LlmIntent.ClarificationCode.NONE);
    }
}
