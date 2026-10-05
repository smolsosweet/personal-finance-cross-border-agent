package com.example.finance;

import com.microsoft.playwright.*;
import java.nio.file.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
    "server.port=8136","spring.datasource.url=jdbc:h2:mem:ui_product;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "app.demo-tools-enabled=true","app.shared-demo=true"})
class UiProductRefinementPlaywrightTest {
    @Autowired DemoDataService demo;
    @Autowired PhaseFourService payments;
    @Autowired CrossBorderService border;
    @Autowired org.springframework.jdbc.core.JdbcTemplate db;
    @MockitoBean LlmIntentClient llm;
    Playwright pw; Browser browser; Page page;
    @BeforeEach void setup() {
        demo.resetAll(); reset(llm); when(llm.enabled()).thenReturn(true);
        pw=Playwright.create();browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
        page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1440,1000));
        page.setDefaultTimeout(12000);page.onDialog(Dialog::accept);
        page.onPageError(error -> fail(error));page.navigate("http://localhost:8136");
    }
    @AfterEach void close(){if(browser!=null)browser.close();if(pw!=null)pw.close();}
    void shot(String stage,String name,int width) throws Exception {
        Path path=Path.of("docs/ui-ux/"+stage+"/"+name+"-"+width+".png");
        Files.createDirectories(path.getParent());page.evaluate("() => window.scrollTo({top:0,behavior:'instant'})");page.screenshot(new Page.ScreenshotOptions().setPath(path));
    }
    @Test void navigationIntroductionAndEnvironmentToolsAreDiscoverable() {
        assertThat(page.getByTestId("environment-label")).hasText("Simulation environment");
        for(String tab:new String[]{"dashboard","transactions","student","agent"})
            assertThat(page.locator("[data-tab='"+tab+"']:visible").first()).isVisible();
        assertThat(page.locator("[data-first-use]")).isVisible();
        page.locator("[data-dismiss-intro]").click();
        assertThat(page.locator("[data-first-use]")).isHidden();
        page.reload();assertThat(page.locator("[data-first-use]")).isHidden();
        page.getByTestId("tab-student").click();page.getByTestId("tab-dashboard").click();
        assertThat(page.locator("[data-first-use]")).isHidden();
        assertThat(page.getByTestId("reset-demo")).isHidden();
        page.getByTestId("environment-tools").locator("summary").click();
        assertThat(page.getByTestId("reset-demo")).isVisible();
        assertThat(page.getByTestId("reset-demo").locator("..")).hasAttribute("data-confirm-en",java.util.regex.Pattern.compile(".*whole team.*|.*team members.*"));
        page.locator("[data-tab='agent']:visible").first().click();
        assertThat(page.locator(".payment-empty")).containsText("No payment plan selected");
        for(int width:new int[]{1440,768,390,360}) {
            page.setViewportSize(width,900);
            assertTrue((Boolean)page.evaluate("() => document.documentElement.scrollWidth<=innerWidth+1"),"Page fits "+width+"px: "+page.evaluate("() => JSON.stringify(Array.from(document.querySelectorAll('body *')).filter(n => n.getClientRects().length && n.getBoundingClientRect().right>innerWidth+1).map(n => ({tag:n.tagName,cls:n.className,width:n.getBoundingClientRect().width,text:n.textContent.substring(0,80)})).slice(0,15))"));
            assertThat(page.getByTestId("environment-label")).isVisible();
            page.getByTestId("assistant-launcher").click();
            assertThat(page.getByTestId("assistant-conversation-input")).isVisible();
            page.getByTestId("assistant-close").click();
        }
        verify(llm,never()).classify(anyString());
    }

    @Test void balancesBudgetsAndTransactionAmountsKeepSourcesAndCurrenciesDistinct() {
        String id=db.queryForObject("SELECT id FROM transactions ORDER BY id FETCH FIRST 1 ROW ONLY",String.class);
        db.update("UPDATE transactions SET amount=42.25,currency='USD' WHERE id=?",id);
        db.update("UPDATE financial_accounts SET currency='CNY',balance=82.25 WHERE id='VCB_VND'");
        page.reload();
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        assertThat(page.getByTestId("current-account-balances")).containsText("82.25 CNY");
        assertThat(page.getByTestId("current-account-balances")).containsText("Each source is counted once");
        assertThat(page.getByTestId("monthly-finance-snapshot")).containsText("Reporting period");
        assertThat(page.getByTestId("monthly-finance-snapshot")).containsText("Budget limits are not money");
        assertThat(page.locator(".cashflow-snapshot")).containsText("no combined total");
        page.locator("[data-tab='transactions']:visible").first().click();
        page.getByTestId("transaction-search").fill("");
        page.getByTestId("transaction-type-filter").selectOption("");
        page.getByTestId("transaction-sort").selectOption("smallest");
        assertThat(page.getByTestId("transaction-row").filter(new Locator.FilterOptions().setHasText("42.25"))).containsText("USD");
        for(int width:new int[]{1440,768,390,360}) {
            page.setViewportSize(width,900);
            assertTrue((Boolean)page.evaluate("() => document.documentElement.scrollWidth<=innerWidth+1"),"Transactions fit "+width);
        }
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
        verify(llm,never()).classify(anyString());
    }

    @Test void stateSpecificReviewAndReceiptRemainAuthoritative() {
        var plan=payments.createTuitionPlan("BANK_A");
        page.navigate("http://localhost:8136/?action="+plan.id()+"#agent-workspace");
        assertThat(page.locator("[data-first-use]")).isHidden();
        assertThat(page.getByTestId("payment-summary")).containsText("70,760,800 VND");
        assertThat(page.getByTestId("payment-summary")).containsText("Bank A Everyday");
        assertThat(page.getByTestId("approval-note")).containsText("Your approval is required");
        assertThat(page.getByTestId("payment-review")).containsText("Current balance");
        assertThat(page.getByTestId("payment-review")).containsText("Estimated balance after payment");
        assertEquals(0,payments.sandboxTransactionCount());assertNull(payments.receiptForAction(plan.id()));
        page.getByTestId("approve-action").click();
        page.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')");
        assertThat(page.getByTestId("latest-receipt")).isVisible();
        assertThat(page.getByTestId("latest-action")).hasAttribute("data-status","COMPLETED");
        assertThat(page.getByTestId("approval-note")).not().containsText("is required");
        assertThat(page.getByTestId("approve-action")).hasCount(0);
        assertThat(page.getByTestId("latest-receipt")).containsText("Balance immediately after this payment");
        assertThat(page.getByTestId("latest-receipt")).containsText("historical receipt balance");
        assertThat(page.getByTestId("latest-receipt")).containsText("No real money is transferred.");
        assertEquals(1,payments.sandboxTransactionCount());
        String receiptId=page.getByTestId("latest-receipt").getAttribute("data-transaction-id");
        payments.approveAndExecute(plan.id());page.reload();
        assertEquals(1,payments.sandboxTransactionCount());
        assertThat(page.getByTestId("latest-receipt")).hasAttribute("data-transaction-id",receiptId);
        assertThat(page.locator(".payment-info")).not().hasAttribute("open","");
        verify(llm,never()).classify(anyString());
    }

    @Test void expiredUnavailableInvalidatedAndEmergencyStatesKeepRecoveryVisible() {
        page.getByTestId("tab-student").click();
        assertThat(page.getByTestId("channel-BANK_B").locator("form")).hasCount(0);
        assertThat(page.getByTestId("channel-BANK_B")).containsText("Not connected");
        page.getByTestId("plan-BANK_A").click();
        page.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')");
        String action=page.getByTestId("latest-action").getAttribute("data-action-id");
        db.update("UPDATE fx_quotes SET expires_at=?",java.time.LocalDateTime.now().minusSeconds(2));
        db.update("UPDATE action_payment_snapshots SET expires_at=? WHERE action_id=?",java.time.LocalDateTime.now().minusSeconds(2),action);
        page.reload();
        assertThat(page.getByTestId("approve-action")).hasCount(0);
        assertThat(page.getByTestId("refresh-expired-plan-quote")).isVisible();
        assertEquals(0,payments.sandboxTransactionCount());
        db.update("UPDATE action_plans SET status='INVALIDATED' WHERE id=?",action);
        page.reload();
        assertThat(page.getByTestId("invalidated-plan")).containsText("previous approval cannot be reused");
        assertThat(page.getByTestId("approve-action")).hasCount(0);
        assertThat(page.getByTestId("approval-note")).hasCount(0);
        page.getByTestId("return-to-comparison").click();
        assertThat(page.getByTestId("channel-BANK_A").locator("[data-channel-refresh]")).isVisible();
        page.getByTestId("emergency-stop").click();
        page.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')");
        assertEquals("PAUSED",payments.policy().state());
        assertThat(page.getByTestId("emergency-resume")).isVisible();
        assertEquals(0,payments.sandboxTransactionCount());
        verify(llm,never()).classify(anyString());
    }

    @Test void bilingualLabelsUserTextDialogsAndMobileTargetsStayUsable() {
        db.update("UPDATE international_bills SET title='Active Tuition Budget Test' WHERE selected=TRUE");
        page.reload();page.getByTestId("language-vi").click();
        assertThat(page.getByTestId("environment-label")).hasText("Môi trường mô phỏng");
        assertThat(page.getByTestId("current-account-balances")).containsText("Số dư tài khoản hiện tại");
        page.getByTestId("tab-student").click();
        assertThat(page.getByTestId("tuition-bill")).containsText("Active Tuition Budget Test");
        assertThat(page.getByTestId("tuition-progress").locator(".current")).containsText("So sánh kênh");
        Locator opener=page.locator("[data-open-dialog='add-student-expense']");
        opener.click();
        assertTrue((Boolean)page.evaluate("() => document.querySelector('dialog[open]').contains(document.activeElement)"));
        page.locator("dialog[open] button[type='submit']").click();
        assertThat(page.locator("dialog[open] [aria-invalid='true']").first()).isVisible();
        page.keyboard().press("Escape");
        assertThat(opener).isFocused();
        page.getByTestId("assistant-launcher").click();
        page.getByTestId("assistant-conversation-input").fill("Active Budget Tuition — nguyên văn");
        page.getByTestId("assistant-language-en").click();
        assertThat(page.getByTestId("assistant-conversation-input")).hasValue("Active Budget Tuition — nguyên văn");
        page.locator("[data-assistant-navigation]").selectOption("agent");
        assertThat(page.getByTestId("assistant-panel")).isHidden();
        assertThat(page.getByTestId("tab-agent")).hasAttribute("aria-selected","true");
        page.getByTestId("assistant-launcher").click();
        assertThat(page.getByTestId("assistant-conversation-input")).hasValue("Active Budget Tuition — nguyên văn");
        for(int width:new int[]{360,390,768,1440}) {
            page.setViewportSize(width,844);
            assertTrue((Boolean)page.evaluate("() => document.documentElement.scrollWidth<=innerWidth+1"),"Assistant fits "+width);
            assertThat(page.getByTestId("assistant-send-message")).isVisible();
            if(width<700)assertTrue(((Number)page.getByTestId("assistant-send-message").evaluate("e=>e.getBoundingClientRect().height")).doubleValue()>=44);
        }
        page.keyboard().press("Escape");
        assertThat(page.getByTestId("assistant-panel")).isHidden();
        verify(llm,never()).classify(anyString());
    }

    @Test void captureWorkspaceScreens() throws Exception {
        String stage=System.getProperty("ui.refinement.stage","after");
        for(int width:new int[]{1440,768,390,360}) {
            page.setViewportSize(width,width==390?844:1000);
            page.navigate("http://localhost:8136");
            for(String tab:new String[]{"dashboard","transactions","student"}) {
                page.locator("[data-tab='"+tab+"']:visible").first().click();shot(stage,tab,width);
                assertTrue((Boolean)page.evaluate("() => document.documentElement.scrollWidth<=innerWidth+1"),tab+" fits "+width);
            }
            var plan=payments.createTuitionPlan("BANK_A");
            page.navigate("http://localhost:8136/?action="+plan.id()+"#agent-workspace");
            shot(stage,"review",width);
            assertTrue((Boolean)page.evaluate("() => document.documentElement.scrollWidth<=innerWidth+1"),"Review fits "+width);
            page.getByTestId("assistant-launcher").click();shot(stage,"assistant",width);
            page.getByTestId("assistant-close").click();
            payments.approveAndExecute(plan.id());
            page.reload();shot(stage,"receipt",width);
            assertTrue((Boolean)page.evaluate("() => document.documentElement.scrollWidth<=innerWidth+1"),"Receipt fits "+width);
            page.locator("[data-language='vi']:visible").click();shot(stage,"receipt-vi",width);
            demo.resetAll();
        }
        verify(llm,never()).classify(anyString());
    }
}
