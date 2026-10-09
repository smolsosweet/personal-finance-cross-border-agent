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

/** Real Chrome/application; mock interpretation permits controlled concurrency and hostile model output. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
    "server.port=8103","spring.datasource.url=jdbc:h2:mem:context_browser;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.demo-tools-enabled=false"})
class ContextualConversationPlaywrightTest {
    @Autowired DemoDataService demo;
    @Autowired PhaseFourService payments;
    @Autowired CrossBorderService crossBorder;
    @Autowired JdbcTemplate db;
    @MockitoBean LlmIntentClient llm;
    private Playwright playwright;
    private Browser browser;
    private Page page;
    private final ArrayList<String> errors=new ArrayList<>();
    @BeforeEach void setup(){
        reset(llm);when(llm.enabled()).thenReturn(true);demo.resetAll();
        playwright=Playwright.create();browser=playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
        page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1440,1000));page.setDefaultTimeout(10000);page.onDialog(Dialog::accept);page.onPageError(errors::add);
        page.navigate("http://localhost:8103");page.getByTestId("language-vi").click();
    }
    @AfterEach void close(){if(browser!=null)browser.close();if(playwright!=null)playwright.close();assertTrue(errors.isEmpty(),String.join(" | ",errors));}
    private LlmIntent intent(LlmIntent.Intent value){return new LlmIntent(value,LlmIntent.ChannelPreference.NONE,new BigDecimal("0.95"),value==LlmIntent.Intent.NEED_CLARIFICATION?LlmIntent.ClarificationCode.AMBIGUOUS_REQUEST:LlmIntent.ClarificationCode.NONE);}
    private void stub(LlmIntent.Intent value){when(llm.classify(anyString())).thenReturn(intent(value));}
    private void idle(){page.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')");}
    private void send(String message){page.getByTestId("assistant-conversation-input").fill(message);page.getByTestId("assistant-send-message").click();idle();}

    @Test void targetedChoicesAndSessionIsolationWorkThroughTheSharedPanel(){
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        stub(LlmIntent.Intent.NEED_CLARIFICATION);page.getByTestId("assistant-launcher").click();send("Còn bao nhiêu?");
        assertThat(page.getByTestId("assistant-context-choice")).hasCount(3);
        page.getByTestId("assistant-conversation-input").fill("Còn bao nhiêu?");
        page.getByTestId("assistant-context-choice").filter(new Locator.FilterOptions().setHasText("Ngân sách")).click();idle();
        assertThat(page.getByTestId("assistant-conversation-input")).hasValue("Còn bao nhiêu?");
        stub(LlmIntent.Intent.EXPLAIN_BUDGET_STATUS);page.getByTestId("assistant-send-message").click();idle();
        assertThat(page.getByTestId("assistant-replies")).containsText("Nguồn: ngân sách cấu hình");
        try(BrowserContext other=browser.newContext()){
            Page second=other.newPage();second.navigate("http://localhost:8103");second.getByTestId("assistant-launcher").click();
            assertThat(second.getByTestId("assistant-replies")).not().containsText("Còn bao nhiêu?");
            assertThat(second.getByTestId("assistant-context-state")).hasAttribute("data-topic","NONE");
        }
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/context-choices-browser-vi.png")));
    }
    @Test void contextualEntryPointsBindTheExactBillAndPlanAndDoNotPay(){
        stub(LlmIntent.Intent.EXPLAIN_TUITION_AFFORDABILITY);
        page.getByTestId("tab-student").click();page.getByTestId("assistant-student-help").click();idle();
        assertThat(page.getByTestId("assistant-context-state")).hasAttribute("data-topic","TUITION_AFFORDABILITY");
        assertThat(page.getByTestId("assistant-conversation-input")).hasValue("Nếu đóng học phí thì còn đủ tiền sinh hoạt không?");
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);page.getByTestId("assistant-send-message").click();idle();
        assertThat(page.getByTestId("assistant-replies")).containsText("ƯỚC TÍNH SAU HỌC PHÍ");assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
        stub(LlmIntent.Intent.CREATE_TUITION_PLAN);send("Chuẩn bị kế hoạch học phí");var plan=payments.latestAction();assertNotNull(plan);
        page.getByTestId("assistant-review-plan").click();page.getByTestId("assistant-plan-help").click();idle();
        stub(LlmIntent.Intent.CHECK_TUITION_STATUS);send("Trạng thái thế nào?");
        assertThat(page.getByTestId("assistant-replies")).containsText(plan.id());assertThat(page.getByTestId("assistant-replies")).containsText("AWAITING_APPROVAL");
        assertEquals(0,payments.sandboxTransactionCount());assertNull(payments.receiptForAction(plan.id()));
        assertThat(page.getByTestId("latest-receipt")).hasCount(0);
    }
    @Test void newChatDraftReplacesPinnedReviewButHistoryStillSelectsTheExactOldPlan(){
        var old=payments.createTuitionPlan("BANK_A");
        crossBorder.refreshQuotes();
        page.navigate("http://localhost:8103/?action="+old.id()+"#agent-workspace");
        page.getByTestId("assistant-launcher").click();
        stub(LlmIntent.Intent.CREATE_TUITION_PLAN);send("Chuẩn bị kế hoạch học phí rẻ nhất.");
        var draft=payments.latestAction();assertNotEquals(old.id(),draft.id());
        assertEquals("INVALIDATED",payments.action(old.id()).status());
        assertThat(page.getByTestId("latest-action")).hasAttribute("data-action-id",draft.id());
        page.getByTestId("assistant-review-plan").click();
        assertThat(page.getByTestId("latest-action")).hasAttribute("data-action-id",draft.id());
        page.getByTestId("assistant-plan-help").click();idle();
        stub(LlmIntent.Intent.CHECK_TUITION_STATUS);send("Trạng thái thế nào?");
        assertThat(page.getByTestId("assistant-replies").locator(".message.assistant").last()).containsText(draft.id()+" · AWAITING_APPROVAL");
        page.getByTestId("assistant-close").click();
        page.navigate("http://localhost:8103/?action="+old.id()+"#agent-workspace");
        page.getByTestId("assistant-plan-help").click();idle();send("Trạng thái thế nào?");
        assertThat(page.getByTestId("assistant-replies").locator(".message.assistant").last()).containsText(old.id()+" · INVALIDATED");
        assertEquals(0,payments.sandboxTransactionCount());assertNull(payments.receiptForAction(draft.id()));
        assertEquals("AWAITING_APPROVAL",payments.action(draft.id()).status());
    }
    @Test void languageSwitchDoesNotPartiallyTranslateRepliesOrRewriteUserMessages(){
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        stub(LlmIntent.Intent.EXPLAIN_BUDGET_STATUS);page.getByTestId("assistant-launcher").click();
        UiLanguageControls.select(page,"en");
        String question="Show budgets. Expense, Refund, Category.";send(question);
        send("How much is left?");UiLanguageControls.select(page,"vi");
        assertThat(page.getByTestId("assistant-replies")).containsText("Category budgets: VND.");
        assertThat(page.getByTestId("assistant-replies")).containsText("Category budgets: VND.");
        assertThat(page.getByTestId("assistant-replies")).not().containsText("Danh mục budgets");
        assertThat(page.getByTestId("assistant-replies").locator(".message.user").first().locator("p")).hasText(question);
        send("Ngân sách tháng này còn bao nhiêu?");
        assertThat(page.getByTestId("assistant-replies")).containsText("Ngân sách danh mục: VND.");
        UiLanguageControls.select(page,"en");
        assertThat(page.getByTestId("assistant-replies")).containsText("Category budgets: VND.");
        assertThat(page.getByTestId("assistant-replies")).containsText("Ngân sách danh mục: VND.");
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
    }
    @Test void contextSwitchAndResetWhileModelWaitsCannotRestoreOldTopicOrCreateDraft(){
        stub(LlmIntent.Intent.EXPLAIN_BUDGET_STATUS);page.getByTestId("assistant-launcher").click();send("Show budgets");
        CountDownLatch release=new CountDownLatch(1);AtomicInteger started=new AtomicInteger(),finished=new AtomicInteger();
        when(llm.classify("How much is left?")).thenAnswer(call->{started.incrementAndGet();assertTrue(release.await(8,TimeUnit.SECONDS));finished.incrementAndGet();return intent(LlmIntent.Intent.EXPLAIN_BUDGET_STATUS);});
        page.getByTestId("assistant-conversation-input").fill("How much is left?");page.getByTestId("assistant-send-message").click();page.waitForCondition(()->started.get()==1);
        page.getByTestId("assistant-close").click();page.getByTestId("tab-student").click();page.getByTestId("assistant-student-help").click();idle();
        assertThat(page.getByTestId("assistant-context-state")).hasAttribute("data-topic","TUITION_AFFORDABILITY");
        release.countDown();page.waitForCondition(()->finished.get()==1);page.waitForTimeout(250);
        assertThat(page.getByTestId("assistant-context-state")).hasAttribute("data-topic","TUITION_AFFORDABILITY");assertThat(page.getByTestId("assistant-send-message")).isEnabled();
        CountDownLatch draftRelease=new CountDownLatch(1);
        when(llm.classify("Prepare tuition draft")).thenAnswer(call->{started.incrementAndGet();assertTrue(draftRelease.await(8,TimeUnit.SECONDS));finished.incrementAndGet();return intent(LlmIntent.Intent.CREATE_TUITION_PLAN);});
        page.getByTestId("assistant-conversation-input").fill("Prepare tuition draft");page.getByTestId("assistant-send-message").click();page.waitForCondition(()->started.get()==2);
        page.getByTestId("assistant-close").click();page.getByTestId("tab-dashboard").click();page.getByTestId("environment-tools").evaluate("e=>e.open=true");page.getByTestId("reset-demo").click();idle();
        draftRelease.countDown();page.waitForCondition(()->finished.get()==2);page.waitForTimeout(250);page.getByTestId("assistant-launcher").click();
        assertThat(page.getByTestId("assistant-context-state")).hasAttribute("data-topic","NONE");assertThat(page.getByTestId("assistant-context-choice")).hasCount(0);
        assertThat(page.getByTestId("assistant-conversation-input")).isEnabled();assertThat(page.getByTestId("assistant-replies")).not().containsText("Prepared tuition-payment plan");
        assertNull(payments.latestAction());assertEquals(0,payments.sandboxTransactionCount());
    }
}
