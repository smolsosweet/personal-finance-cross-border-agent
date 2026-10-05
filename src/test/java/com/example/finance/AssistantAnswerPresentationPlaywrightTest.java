package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.microsoft.playwright.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Actual app and Chrome; intent classification mocked. No real financial data or payments. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
    "server.port=8117", "spring.datasource.url=jdbc:h2:mem:answer_presentation;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "app.demo-tools-enabled=false"})
class AssistantAnswerPresentationPlaywrightTest {
    @Autowired DemoDataService demo;
    @Autowired JdbcTemplate db;
    @Autowired PhaseFourService payments;
    @MockitoBean LlmIntentClient llm;
    Playwright pw; Browser browser; Page page;
    final ArrayList<String> errors=new ArrayList<>();
    @BeforeEach void setup(){
        reset(llm);when(llm.enabled()).thenReturn(true);demo.resetAll();
        pw=Playwright.create();browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
        page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1440,1000));page.setDefaultTimeout(15000);
        page.onPageError(errors::add);page.onDialog(Dialog::accept);page.navigate("http://localhost:8117");page.getByTestId("assistant-launcher").click();
    }
    @AfterEach void close(){if(browser!=null)browser.close();if(pw!=null)pw.close();assertTrue(errors.isEmpty(),String.join(" | ",errors));}
    void stub(LlmIntent.Intent value){stub(value,LlmIntent.ChannelPreference.NONE);}
    void stub(LlmIntent.Intent value,LlmIntent.ChannelPreference pref){when(llm.classify(anyString())).thenReturn(new LlmIntent(value,pref,new BigDecimal("0.95"),LlmIntent.ClarificationCode.NONE));}
    void idle(){page.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')");}
    void send(String text){page.getByTestId("assistant-conversation-input").fill(text);page.getByTestId("assistant-send-message").click();idle();}
    Locator reply(){return page.getByTestId("assistant-replies").locator(".message.assistant").last();}
    void confirm(String amount){page.getByTestId("runway-monthly-amount").fill(amount);page.getByTestId("runway-confirm").click();idle();}

    @Test void bilingualBudgetsHaveConclusionTableCollapsedEvidenceAndEscapedCategory(){
        db.update("UPDATE transactions SET occurred_at=?",LocalDateTime.of(2019,1,1,12,0));
        db.update("UPDATE budgets SET category=? WHERE category='Shopping'","<img src=x onerror=alert(1)>");
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);stub(LlmIntent.Intent.EXPLAIN_BUDGET_STATUS);
        page.getByTestId("assistant-language-vi").click();send("Ngân sách tháng này còn bao nhiêu?");
        assertThat(reply().getByTestId("response-conclusion")).hasText("Ngân sách còn 6.300.000 VND");
        assertThat(reply().getByTestId("response-table").locator("tbody tr")).hasCount(4);
        assertThat(reply().getByTestId("response-table")).containsText("<img src=x onerror=alert(1)>");
        assertThat(reply().locator("img")).hasCount(0);
        assertThat(reply().getByTestId("response-warning")).containsText("không phải số dư tài khoản");
        assertThat(reply().locator("[data-response-raw]")).isHidden();
        reply().getByTestId("response-details").locator("summary").click();
        assertThat(reply().locator("[data-response-raw]")).isVisible();
        assertThat(reply().locator("[data-response-raw]")).containsText("Nguồn: ngân sách cấu hình");
        page.getByTestId("assistant-language-en").click();send("Show my remaining budgets");
        assertThat(reply().getByTestId("response-conclusion")).hasText("Budget remaining: 6,300,000 VND");
        assertThat(reply().getByTestId("response-table").locator("thead")).hasText("CategoryLimitSpentRemainingOverspent");
        assertThat(reply().getByTestId("response-details").locator("summary")).hasText("View details");
        page.getByTestId("assistant-language-vi").click();
        assertThat(reply().getByTestId("response-warning")).containsText("Remaining budget is not an account balance");
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
        page.getByTestId("assistant-panel").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/answer-ux-budget.png")));
    }
    @Test void spendingKeepsCurrenciesAndRefundsSeparateWithoutRoundingAmounts(){
        db.update("UPDATE transactions SET occurred_at=?",LocalDateTime.of(2019,1,1,12,0));
        var ids=db.queryForList("SELECT id FROM transactions WHERE type='Expense' ORDER BY id FETCH FIRST 2 ROWS ONLY",String.class);
        db.update("UPDATE transactions SET occurred_at=?,amount=100000.25,currency='VND',category='Food & Drinks',review_status='CONFIRMED' WHERE id=?",LocalDateTime.now(),ids.get(0));
        db.update("UPDATE transactions SET occurred_at=?,amount=200.50,currency='CNY',category='Education',review_status='CONFIRMED' WHERE id=?",LocalDateTime.now(),ids.get(1));
        db.update("UPDATE transactions SET occurred_at=?,amount=30.50 WHERE type='Refund'",LocalDateTime.now());
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);stub(LlmIntent.Intent.EXPLAIN_SPENDING_SUMMARY);send("Show spending this month");
        assertThat(reply().getByTestId("response-conclusion")).hasText("Spending by currency");
        assertThat(reply().getByTestId("response-table").locator("tbody tr")).hasCount(2);
        assertThat(reply().getByTestId("response-table")).containsText("100,000.25");
        assertThat(reply().getByTestId("response-table")).containsText("200.50");
        assertThat(reply().getByTestId("response-table")).not().containsText("Refunds");
        assertThat(reply().locator(".response-facts")).containsText("Refunds recorded separately · VND");
        assertThat(reply().locator(".response-facts")).containsText("30.50 VND");
        assertThat(reply().getByTestId("response-warning")).containsText("Currencies are not added");
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
    }
    @Test void runwayChunksStayOneAnswerAndFormAndComposerFitReducedVisualViewport(){
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);stub(LlmIntent.Intent.EXPLAIN_LIVING_EXPENSE_RUNWAY);
        page.getByTestId("assistant-language-vi").click();send("Sau khi đóng học phí đủ sinh hoạt mấy tháng?");confirm("8000000");
        assertThat(reply().getByTestId("response-conclusion")).hasText("Đủ khoảng 3,27 tháng");
        assertThat(reply().getByTestId("response-estimate")).hasText("ƯỚC TÍNH");
        assertThat(reply().locator(".response-facts")).containsText("26.239.200 VND");
        assertThat(reply().locator("[data-response-raw]")).containsText("kế hoạch và thanh toán dùng cùng sổ tài khoản");
        assertThat(reply().locator("[data-response-raw]")).isHidden();
        // Adjacent structured confirmations are separate replies, even without a user chat message.
        confirm("10000000");assertThat(reply().getByTestId("response-conclusion")).hasText("Đủ khoảng 2,62 tháng");
        assertThat(page.getByTestId("assistant-replies").getByTestId("response-conclusion")).hasCount(2);
        page.setViewportSize(390,844);
        page.getByTestId("assistant-panel").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/answer-ux-runway-mobile.png")));
        // Emulate keyboard viewport geometry; this is not a physical-device keyboard test.
        for(int height:new int[]{430,337}){
        page.evaluate("""
          height => {const viewport=new EventTarget();Object.assign(viewport,{height,width:390,offsetTop:18,offsetLeft:0});
            Object.defineProperty(window,'visualViewport',{value:viewport,configurable:true});window.dispatchEvent(new Event('resize'));}
          """,height);
        page.getByTestId("assistant-panel").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/answer-ux-keyboard-geometry-"+height+".png")));
        for(String testId:new String[]{"runway-monthly-amount","runway-confirm","assistant-conversation-input","assistant-send-message"}){
            assertTrue((Boolean)page.getByTestId(testId).evaluate("node => { const r=node.getBoundingClientRect(),v=window.visualViewport; const at=document.elementFromPoint(r.left+r.width/2,r.top+r.height/2);return r.top>=v.offsetTop && r.bottom<=v.offsetTop+v.height && (at===node || node.contains(at)); }"),testId+" is inside the visible viewport and not covered");
        }
        }
        confirm("9000000");assertThat(reply().getByTestId("response-conclusion")).hasText("Đủ khoảng 2,91 tháng");
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertNull(payments.latestAction());assertNull(payments.latestReceipt());
    }
    @Test void completedTuitionAnswerShowsReceiptBalanceAsAnExecutedResult(){
        var plan=payments.createTuitionPlan("BANK_A");var receipt=payments.approveAndExecute(plan.id());assertNotNull(receipt);
        db.update("UPDATE fx_quotes SET expires_at=?",LocalDateTime.now().minusMinutes(1));
        page.getByTestId("assistant-language-vi").click();stub(LlmIntent.Intent.EXPLAIN_TUITION_AFFORDABILITY);

        send("Sau khi đã đóng học phí còn bao nhiêu tiền?");

        assertThat(reply().getByTestId("response-conclusion")).hasText("Số dư sau thanh toán: 29.239.200 VND");
        assertThat(reply().getByTestId("response-estimate")).hasCount(0);
        assertThat(reply().getByTestId("response-warning")).containsText("kết quả đã thực thi");
        assertThat(reply().locator("[data-response-raw]")).containsText(receipt.transactionId());
        assertThat(reply().locator("[data-response-raw]")).containsText("Không cần báo giá đang hiệu lực");
        assertEquals(1,payments.sandboxTransactionCount());
        assertEquals(receipt.transactionId(),payments.receiptForAction(plan.id()).transactionId());
    }
    @Test void channelCardsExplainActualCountRankingAndExpiryWithoutCreatingPlan(){
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        stub(LlmIntent.Intent.COMPARE_TUITION_CHANNELS,LlmIntent.ChannelPreference.CHEAPEST);send("Compare cheapest tuition channels");
        assertThat(reply().getByTestId("response-conclusion")).hasText("Showing 3 eligible channels · lowest total cost");
        assertThat(reply().getByTestId("response-channel")).hasCount(3);
        assertThat(reply().getByTestId("response-channel").first()).containsText("Bank A International Transfer");
        assertThat(reply().getByTestId("response-channel").first()).containsText("70,760,800 VND");
        assertThat(reply().getByTestId("response-channels")).not().containsText("Bank B");
        assertThat(reply().getByTestId("response-warning")).containsText("requires separate approval");
        assertThat(reply().locator("[data-response-raw]")).isHidden();
        page.getByTestId("assistant-language-vi").click();stub(LlmIntent.Intent.COMPARE_TUITION_CHANNELS,LlmIntent.ChannelPreference.FASTEST);send("So sánh các kênh học phí nhanh nhất");
        assertThat(reply().getByTestId("response-conclusion")).containsText("Hiển thị 3 kênh đủ điều kiện · nhanh nhất");
        assertThat(reply().getByTestId("response-channel").first()).containsText("Alipay");
        page.getByTestId("assistant-panel").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/answer-ux-comparison.png")));
        // Browser clock only: verify countdown becomes a visible expired warning without F5.
        page.clock().install();page.clock().fastForward(6*60*1000);
        assertThat(reply().locator("[data-response-quote]").first()).containsText("Báo giá đã hết hạn");
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertNull(payments.latestAction());assertEquals(0,payments.sandboxTransactionCount());
    }
    @Test void shortfallAndUnifiedProjectionAssumptionsRemainVisibleOutsideDetails(){
        db.update("UPDATE financial_accounts SET balance=10000000 WHERE id='CHECKING'");var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        page.getByTestId("assistant-language-vi").click();stub(LlmIntent.Intent.EXPLAIN_TUITION_AFFORDABILITY);send("Nếu đóng học phí thì còn đủ tiền sinh hoạt không?");
        assertThat(reply().getByTestId("response-estimate")).isVisible();
        assertThat(reply().getByTestId("response-warning").filter(new Locator.FilterOptions().setHasText("số dư chung"))).isVisible();
        assertThat(reply().getByTestId("response-warning").filter(new Locator.FilterOptions().setHasText("không đủ đệm an toàn"))).isVisible();
        stub(LlmIntent.Intent.EXPLAIN_LIVING_EXPENSE_RUNWAY);send("Sau học phí đủ sinh hoạt mấy tháng?");confirm("8000000");
        assertThat(reply().getByTestId("response-warning").filter(new Locator.FilterOptions().setHasText("THIẾU TIỀN"))).isVisible();
        assertThat(reply().getByTestId("response-conclusion")).hasText("Đủ khoảng 0,00 tháng");
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertNull(payments.latestAction());assertNull(payments.latestReceipt());
    }
    @Test void backendExpiredQuoteErrorStaysVisibleAndUncollapsed(){
        db.update("UPDATE fx_quotes SET expires_at=?",LocalDateTime.now().minusMinutes(1));var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        page.getByTestId("assistant-language-vi").click();stub(LlmIntent.Intent.EXPLAIN_LIVING_EXPENSE_RUNWAY);send("Sau học phí đủ sinh hoạt mấy tháng?");
        assertThat(reply().locator("[data-response-raw]")).isVisible();assertThat(reply()).containsText("Báo giá đã hết hạn");
        assertThat(reply().getByTestId("response-details")).hasCount(0);assertThat(reply().getByTestId("response-conclusion")).hasCount(0);
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertNull(payments.latestAction());
    }
}
