package com.example.finance;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.microsoft.playwright.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Real Chrome and application, synthetic database; only model classification is mocked. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
    "server.port=8101",
    "spring.datasource.url=jdbc:h2:mem:assistant_panel;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "app.demo-tools-enabled=false"
})
class AssistantPanelPlaywrightTest {
    @Autowired DemoDataService demo;
    @Autowired PhaseFourService payments;
    @Autowired JdbcTemplate db;
    @MockitoBean LlmIntentClient llm;
    private Playwright playwright;
    private Browser browser;
    private Page page;
    private final ArrayList<String> errors = new ArrayList<>();

    @BeforeEach void setup() {
        reset(llm);
        when(llm.enabled()).thenReturn(true);
        demo.resetAll();
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
        page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 1000));
        page.setDefaultTimeout(10000);
        page.onPageError(errors::add);
        page.onDialog(Dialog::accept);
        page.navigate("http://localhost:8101");
    }

    @AfterEach void close() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
        assertTrue(errors.isEmpty(), String.join(" | ", errors));
    }

    @Test void entryPointsLanguageAndDraftSurviveTabChangesWithoutCallingModel() {
        var before = PersonalFinanceAiIntegrationTest.snapshot(db);
        assertThat(page.getByTestId("assistant-launcher")).isVisible();
        assertThat(panel()).isHidden();
        page.getByTestId("tab-transactions").click();
        page.getByTestId("assistant-launcher").click();
        assertThat(page.getByTestId("assistant-screen")).hasText("Transactions");
        page.getByTestId("assistant-question-spending").click();
        assertThat(input()).hasValue("Where did I spend the most this month?");
        input().fill("My unfinished question");
        page.getByTestId("assistant-close").click();
        page.getByTestId("tab-student").click();
        page.getByTestId("assistant-launcher").click();
        assertThat(input()).hasValue("My unfinished question");
        assertThat(page.getByTestId("assistant-screen")).hasText("Student finance");
        page.getByTestId("assistant-language-vi").click();
        assertThat(page.locator("#assistant-title")).hasText("Hỏi FinBridge");
        assertThat(page.getByTestId("assistant-screen")).hasText("Tài chính du học");
        assertThat(input()).hasValue("My unfinished question");
        page.getByTestId("assistant-close").click();
        double top = ((Number) page.evaluate("() => window.scrollY")).doubleValue();
        page.getByTestId("assistant-student-help").click();
        assertThat(input()).hasValue("Nếu đóng học phí thì còn đủ tiền sinh hoạt không?");
        assertEquals(top, ((Number) page.evaluate("() => window.scrollY")).doubleValue(), 1);
        panel().screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/assistant-student-desktop-vi.png")));
        verify(llm, never()).classify(anyString());
        assertEquals(before, PersonalFinanceAiIntegrationTest.snapshot(db));
    }

    @Test void pendingStateDuplicateGuardAndRepliesAreSharedWithOverview() {
        var before = PersonalFinanceAiIntegrationTest.snapshot(db);
        AtomicInteger calls = new AtomicInteger();
        when(llm.classify(anyString())).thenAnswer(invocation -> {
            calls.incrementAndGet(); Thread.sleep(1300);
            return intent(LlmIntent.Intent.EXPLAIN_BUDGET_STATUS);
        });
        page.getByTestId("language-vi").click();
        page.getByTestId("tab-student").click();
        page.getByTestId("assistant-launcher").click();
        send("Ngân sách tháng này của tôi còn bao nhiêu?");
        assertThat(input()).isDisabled();
        assertThat(page.getByTestId("assistant-send-message")).isDisabled();
        assertThat(panel().locator("[data-chat-processing]")).containsText("Đang xử lý");
        page.evaluate("""
            () => { const f=document.querySelector('[data-assistant-panel] form[data-chat-form]');
              f.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));
              f.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true})); }
            """);
        page.getByTestId("assistant-close").click();
        page.getByTestId("tab-transactions").click();
        page.getByTestId("assistant-launcher").click();
        idle();
        assertThat(page.getByTestId("assistant-replies")).containsText("Nguồn: ngân sách cấu hình");
        assertThat(page.getByTestId("assistant-replies")).containsText("Nguồn: ngân sách cấu hình");
        assertThat(page.getByTestId("assistant-screen")).hasText("Giao dịch");
        assertThat(input()).isEnabled();
        assertThat(input()).hasValue("");
        assertEquals(1, calls.get());
        assertEquals(before, PersonalFinanceAiIntegrationTest.snapshot(db));
        var reply = page.getByTestId("assistant-replies").locator(".message.assistant").last();
        assertTrue((Boolean) reply.evaluate("node => { const r=node.getBoundingClientRect(), p=node.closest('.assistant-body').getBoundingClientRect(); return r.top>=p.top-1 && r.top<p.bottom; }"));
    }

    @Test void connectionFailureAndTimeoutKeepDraftAndRecoverOnlyAfterExplicitRetry() {
        var before = PersonalFinanceAiIntegrationTest.snapshot(db);
        when(llm.classify(anyString())).thenReturn(intent(LlmIntent.Intent.EXPLAIN_SPENDING_SUMMARY));
        page.getByTestId("assistant-launcher").click();
        page.route("**/agent/message", route -> route.abort());
        send("Where did I spend the most this month?");
        idle();
        assertThat(panel().locator(".request-error")).containsText("No automatic retry");
        assertThat(input()).isEnabled();
        assertThat(input()).hasValue("Where did I spend the most this month?");
        verify(llm, never()).classify(anyString());
        page.unroute("**/agent/message");
        page.route("**/agent/message", route -> {});
        panel().locator("form[data-chat-form]").evaluate("f => f.dataset.chatTimeout='250'");
        page.getByTestId("assistant-send-message").click();
        idle();
        assertThat(input()).isEnabled();
        page.unroute("**/agent/message");
        page.getByTestId("assistant-send-message").click();
        idle();
        assertThat(panel().locator(".request-error")).hasCount(0);
        assertThat(page.getByTestId("assistant-replies")).containsText("Evidence: recorded transactions");
        verify(llm, times(1)).classify(anyString());
        assertEquals(before, PersonalFinanceAiIntegrationTest.snapshot(db));
    }

    @Test void draftNeedsSeparateApprovalAndContextUsesExactlyTheDisplayedPlan() {
        when(llm.classify(anyString())).thenReturn(intent(LlmIntent.Intent.CREATE_TUITION_PLAN));
        page.getByTestId("tab-student").click();
        page.getByTestId("assistant-launcher").click();
        send("Prepare the cheapest tuition payment draft.");
        idle();
        var plan = payments.latestAction();
        assertNotNull(plan);
        assertEquals("AWAITING_APPROVAL", plan.status());
        assertEquals("APPROVAL", plan.requiredPermission());
        assertEquals(0, payments.sandboxTransactionCount());
        assertNull(payments.receiptForAction(plan.id()));
        assertThat(page.getByTestId("tab-student")).hasAttribute("aria-selected", "true");
        assertThat(page.getByTestId("assistant-review-plan")).isVisible();
        page.getByTestId("assistant-review-plan").click();
        assertThat(panel()).isHidden();
        assertThat(page.getByTestId("approve-action")).isVisible();
        String total = page.getByTestId("review-total").innerText();
        String remaining = page.getByTestId("review-remaining").innerText();
        var beforeExplanation = PersonalFinanceAiIntegrationTest.snapshot(db);
        page.getByTestId("assistant-plan-help").click();
        assertThat(page.getByTestId("assistant-plan-total")).hasText(total);
        assertThat(page.getByTestId("assistant-plan-remaining")).hasText(remaining);
        assertThat(page.getByTestId("assistant-plan-context")).containsText("explicit approval");
        assertThat(page.getByTestId("payment-demo-tools")).hasCount(0);
        assertEquals(beforeExplanation, PersonalFinanceAiIntegrationTest.snapshot(db));
        verify(llm, times(1)).classify(anyString());
        panel().screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/assistant-plan-desktop-en.png")));
    }

    @Test void mobilePanelFitsLanguageAndKeyboardWorkWithoutExposingDemoControls() {
        payments.createTuitionPlan("ALIPAY");
        page.setViewportSize(390, 844);
        page.navigate("http://localhost:8101/?action=" + payments.latestAction().id() + "#agent-workspace");
        page.getByTestId("assistant-plan-help").click();
        page.getByTestId("assistant-language-vi").click();
        assertThat(page.locator("#assistant-title")).hasText("Hỏi FinBridge");
        assertThat(page.getByTestId("assistant-screen")).hasText("Thanh toán & Lịch sử");
        assertThat(page.getByTestId("assistant-plan-context")).containsText("cần bạn phê duyệt");
        assertThat(page.getByTestId("assistant-emergency-stop")).isVisible();
        assertThat(input()).isVisible();
        assertTrue((Boolean) panel().evaluate("p => { const r=p.getBoundingClientRect(); return r.left>=0 && r.right<=innerWidth && r.top>=0 && r.bottom<=innerHeight && p.scrollWidth<=p.clientWidth+1; }"));
        input().fill("Câu hỏi chưa gửi");
        page.getByTestId("assistant-language-en").click();
        assertThat(page.locator("#assistant-title")).hasText("Ask FinBridge");
        assertThat(input()).hasValue("Câu hỏi chưa gửi");
        page.getByTestId("assistant-language-vi").click();
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/assistant-mobile-vi.png")));
        page.keyboard().press("Escape");
        assertThat(panel()).isHidden();
        assertThat(page.getByTestId("assistant-plan-help")).isFocused();
        assertThat(page.getByTestId("assistant-launcher")).isVisible();
        assertThat(page.getByTestId("payment-demo-tools")).hasCount(0);
        verify(llm, never()).classify(anyString());
        assertEquals(0, payments.sandboxTransactionCount());
    }

    @Test void emergencyStopBlocksLateDraftAndResetDiscardsLateResponseAndRestoresComposer() {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger returned = new AtomicInteger();
        CountDownLatch stopReply = new CountDownLatch(1), resetReply = new CountDownLatch(1);
        when(llm.classify(anyString())).thenAnswer(invocation -> {
            int call = calls.incrementAndGet();
            assertTrue((call == 1 ? stopReply : resetReply).await(8, TimeUnit.SECONDS));
            returned.incrementAndGet();
            return intent(LlmIntent.Intent.CREATE_TUITION_PLAN);
        });
        page.getByTestId("assistant-launcher").click();
        send("Prepare tuition draft");
        page.waitForCondition(() -> calls.get() == 1);
        page.getByTestId("assistant-emergency-stop").click();
        idle();
        assertThat(panel().locator("[data-assistant-policy]")).containsText("Emergency Stop is active");
        assertThat(input()).isEnabled();
        stopReply.countDown();
        page.waitForCondition(() -> returned.get() == 1);
        page.waitForTimeout(300);
        assertEquals("PAUSED", payments.policy().state());
        assertNull(payments.latestAction());
        assertEquals(0, payments.sandboxTransactionCount());
        assertThat(page.getByTestId("assistant-replies")).not().containsText("Prepared tuition-payment plan");
        page.getByTestId("assistant-close").click();
        page.getByTestId("environment-tools").evaluate("e=>e.open=true");        page.getByTestId("reset-demo").click();
        idle();
        page.getByTestId("assistant-launcher").click();
        send("Prepare tuition draft");
        page.waitForCondition(() -> calls.get() == 2);
        page.getByTestId("assistant-close").click();
        page.getByTestId("environment-tools").evaluate("e=>e.open=true");        page.getByTestId("reset-demo").click();
        idle();
        resetReply.countDown();
        page.waitForCondition(() -> returned.get() == 2);
        page.getByTestId("assistant-launcher").click();
        page.waitForTimeout(300);
        assertThat(input()).isEnabled();
        assertThat(panel().locator("[data-chat-processing]")).isHidden();
        assertThat(page.getByTestId("assistant-replies")).not().containsText("Prepared tuition-payment plan");
        assertNull(payments.latestAction());
        assertEquals(0, payments.sandboxTransactionCount());
    }


    @Test void reopeningLongChatRestoresTheLastReadPositionAndDraftAcrossTabs() {
        var before = PersonalFinanceAiIntegrationTest.snapshot(db);
        when(llm.classify(anyString())).thenReturn(intent(LlmIntent.Intent.EXPLAIN_BUDGET_STATUS));
        page.getByTestId("assistant-launcher").click();
        for (int i = 0; i < 8; i++) {
            send("Show remaining budgets this month.");
            idle();
        }
        Locator body = panel().locator(".assistant-body");
        for (double fraction : new double[]{1.0, 0.45, 0.0}) {
            body.evaluate("(node, fraction) => node.scrollTop=(node.scrollHeight-node.clientHeight)*fraction", fraction);
            double beforeClose = ((Number) body.evaluate("node => node.scrollTop")).doubleValue();
            input().fill("Unsent question");
            page.getByTestId("assistant-close").click();
            page.getByTestId("assistant-launcher").click();
            assertEquals(beforeClose, ((Number) body.evaluate("node => node.scrollTop")).doubleValue(), 2);
            assertThat(input()).hasValue("Unsent question");
        }
        body.evaluate("node => node.scrollTop=node.scrollHeight*0.45");
        String anchor = (String) body.evaluate("""
            node => Array.from(node.querySelectorAll('[data-reply-id]'))
              .find(reply => reply.getBoundingClientRect().bottom>node.getBoundingClientRect().top)?.dataset.replyId
            """);
        Locator anchoredReply = body.locator("[data-reply-id=\"" + anchor + "\"]");
        double offset = ((Number) anchoredReply.evaluate("node => node.getBoundingClientRect().top-node.closest('.assistant-body').getBoundingClientRect().top")).doubleValue();
        page.getByTestId("assistant-close").click();
        page.getByTestId("tab-student").click();
        page.getByTestId("assistant-launcher").click();
        assertEquals(offset, ((Number) anchoredReply.evaluate("node => node.getBoundingClientRect().top-node.closest('.assistant-body').getBoundingClientRect().top")).doubleValue(), 2);
        assertEquals(before, PersonalFinanceAiIntegrationTest.snapshot(db));
        assertNull(payments.latestAction());
        panel().screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/assistant-scroll-restored.png")));
    }

    @Test void aReplyArrivingWhileChatIsClosedIsVisibleWhenReopened() {
        var before = PersonalFinanceAiIntegrationTest.snapshot(db);
        when(llm.classify(anyString())).thenReturn(intent(LlmIntent.Intent.EXPLAIN_BUDGET_STATUS));
        page.getByTestId("assistant-launcher").click();
        for (int i = 0; i < 5; i++) {
            send("Show remaining budgets this month.");
            idle();
        }
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        when(llm.classify(anyString())).thenAnswer(invocation -> {
            calls.incrementAndGet();
            assertTrue(release.await(8, TimeUnit.SECONDS));
            return intent(LlmIntent.Intent.EXPLAIN_SPENDING_SUMMARY);
        });
        panel().locator(".assistant-body").evaluate("node => node.scrollTop=0");
        send("Where did I spend the most this month?");
        page.waitForCondition(() -> calls.get() == 1);
        page.getByTestId("assistant-close").click();
        release.countDown();
        idle();
        page.getByTestId("assistant-launcher").click();
        Locator lastReply = page.getByTestId("assistant-replies").locator(".message.assistant").last();
        assertThat(lastReply.getByTestId("response-conclusion")).containsText("Spent");
        assertTrue((Boolean) lastReply.evaluate("node => {const r=node.getBoundingClientRect(),b=node.closest('.assistant-body').getBoundingClientRect();return r.top>=b.top-1 && r.top<b.bottom;}"));
        assertEquals(before, PersonalFinanceAiIntegrationTest.snapshot(db));
    }

    @Test void receiptShortcutStaysVisibleAndOpensTheDisplayedReceiptWithoutExecution() {
        var completedPlan = payments.createTuitionPlan("BANK_A");
        var receipt = payments.approveAndExecute(completedPlan.id());
        assertNotNull(receipt);
        var newerPlan = payments.createLowRiskPlan(new BigDecimal("10000"));
        assertEquals("AWAITING_APPROVAL", newerPlan.status());
        page.navigate("http://localhost:8101/?action=" + completedPlan.id() + "#agent-workspace");
        page.getByTestId("assistant-launcher").click();
        page.getByTestId("assistant-language-vi").click();
        when(llm.classify(anyString())).thenReturn(intent(LlmIntent.Intent.EXPLAIN_BUDGET_STATUS));
        for (int i = 0; i < 6; i++) {
            send("Show remaining budgets this month.");
            idle();
        }
        var beforeNavigation = PersonalFinanceAiIntegrationTest.snapshot(db);
        for (int width : new int[]{1440, 390}) {
            page.setViewportSize(width, width == 390 ? 844 : 1000);
            panel().locator(".assistant-body").evaluate("node => node.scrollTop=node.scrollHeight");
            Locator shortcut = page.getByTestId("assistant-review-plan");
            assertThat(shortcut).hasText("Xem biên nhận");
            assertThat(shortcut).hasAttribute("data-action-id", completedPlan.id());
            assertTrue((Boolean) shortcut.evaluate("""
                node => {const r=node.getBoundingClientRect(),p=node.closest('[data-assistant-panel]').getBoundingClientRect();
                  const hit=document.elementFromPoint(r.left+r.width/2,r.top+r.height/2);
                  return !node.closest('.assistant-body') && r.top>=p.top && r.bottom<=p.bottom && r.right<=innerWidth && (hit===node || node.contains(hit));}
                """));
            panel().screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/assistant-receipt-shortcut-" + width + ".png")));
        }
        page.getByTestId("assistant-language-en").click();
        assertThat(page.getByTestId("assistant-review-plan")).hasText("View receipt");
        page.getByTestId("assistant-review-plan").click();
        assertThat(panel()).isHidden();
        page.waitForCondition(() -> (Boolean) page.getByTestId("latest-receipt").evaluate("node => {const r=node.getBoundingClientRect();return r.top>=0 && r.top<innerHeight;}"));
        assertThat(page.getByTestId("latest-receipt")).hasAttribute("data-transaction-id", receipt.transactionId());
        assertThat(page.getByTestId("tab-agent")).hasAttribute("aria-selected", "true");
        assertTrue((Boolean) page.getByTestId("latest-receipt").evaluate("node => {const r=node.getBoundingClientRect();return r.top>=0 && r.top<innerHeight;}"));
        assertEquals(beforeNavigation, PersonalFinanceAiIntegrationTest.snapshot(db));
        assertEquals(1, payments.sandboxTransactionCount());
        assertNull(payments.receiptForAction(newerPlan.id()));
        assertEquals("AWAITING_APPROVAL", payments.action(newerPlan.id()).status());
    }

    private Locator panel() { return page.getByTestId("assistant-panel"); }
    private Locator input() { return page.getByTestId("assistant-conversation-input"); }
    private void send(String question) {
        input().fill(question);
        page.getByTestId("assistant-send-message").click();
    }
    private void idle() { page.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')"); }
    private LlmIntent intent(LlmIntent.Intent value) {
        return new LlmIntent(value, LlmIntent.ChannelPreference.NONE, new BigDecimal("0.95"), LlmIntent.ClarificationCode.NONE);
    }
}
