package com.example.finance;

import com.microsoft.playwright.*;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Real Chrome automation, isolated synthetic H2 state; no external model or shared hosting. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
    "server.port=8163",
    "spring.datasource.url=jdbc:h2:mem:business_ux;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "app.demo-tools-enabled=true"})
class BusinessUxPlaywrightTest {
    @Autowired DemoDataService demo;
    @Autowired PhaseFourService payments;
    @Autowired CrossBorderService border;
    @Autowired FinanceWorkspaceService workspace;
    @Autowired AccountLedgerService ledger;
    @Autowired JdbcTemplate db;
    @MockitoBean LlmIntentClient model;
    Playwright pw; Browser browser; Page page;
    List<String> errors;
    final List<String> approvalRequests=new ArrayList<>();
    static final String BASE="http://localhost:8163";
    @BeforeEach void open() {
        demo.resetAll(); reset(model); when(model.enabled()).thenReturn(false);
        errors=new ArrayList<>(); approvalRequests.clear();
        pw=Playwright.create();
        browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
        page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1366,900));
        page.setDefaultTimeout(10000); watch(page);
    }
    void watch(Page target) {
        target.onPageError(errors::add);
        target.onRequest(r->{if(r.method().equals("POST")&&r.url().endsWith("/approve"))approvalRequests.add(r.url());});
    }
    @AfterEach void close() {
        if(browser!=null)browser.close(); if(pw!=null)pw.close();
        assertTrue(errors.isEmpty(),errors.toString());
        verify(model,never()).classify(anyString());
    }
    void plan(PhaseFourService.ActionPlan plan) {page.navigate(BASE+"/?action="+plan.id()+"#agent-workspace");}
    void idle() {page.waitForFunction("()=>!document.querySelector('.content').hasAttribute('aria-busy')");}
    void capture(String name)throws Exception {
        Path dir=Path.of("docs/business-ux");Files.createDirectories(dir);
        page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(name+".png")));
    }
    void fits(Page target) {
        assertTrue((Boolean)target.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"),"No page overflow");
    }
    @Test void overviewHasOneVndSummaryAndBalancedCardsAcrossViewports()throws Exception {
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        page.navigate(BASE);
        var summary=workspace.summary();
        for(int width:new int[]{360,390,768,1366,1440,1920}) {
            page.setViewportSize(width,900);
            for(String language:new String[]{"en","vi"}) {
                page.getByTestId("language-"+language).click();
                fits(page);
                assertThat(page.getByTestId("liquidity-summary")).isVisible();
                assertThat(page.getByTestId("total-personal-balance")).hasCount(1);
                assertThat(page.getByTestId("total-personal-balance")).containsText(new ConversationPresentation().money(summary.totalBalance()));
                assertThat(page.getByTestId("planning-safety-buffer")).containsText("3,000,000");
                assertThat(page.getByTestId("reserved-next-30")).containsText(new ConversationPresentation().money(summary.reservedNext30Days()));
                var details=page.locator(".planning-explanation");
                if(!(Boolean)details.evaluate("e=>e.open"))details.locator("summary").click();
                assertThat(details).containsText(language.equals("vi")?"Không cộng tiền tệ khác vào VND":"Other currencies are not added to VND");
                if(width>=1366)assertTrue((Boolean)page.locator(".current-account-grid").evaluate("e=>getComputedStyle(e).gridTemplateColumns.split(' ').length===3"));
                if(width==1366||width==360){page.evaluate("()=>window.scrollTo({top:0,behavior:'instant'})");capture("overview-"+width+"-"+language);}
                assertTrue((Boolean)page.locator(".current-account-grid").evaluate("e=>{const h=Array.from(e.children,x=>x.getBoundingClientRect().height);return Math.max(...h)-Math.min(...h)<2}"),width+" "+language+" "+page.locator(".current-account-grid").evaluate("e=>({heights:Array.from(e.children,x=>x.getBoundingClientRect().height),rows:getComputedStyle(e).gridTemplateRows,auto:getComputedStyle(e).gridAutoRows})"));
            }
        }
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
    }
    @Test void finalReviewHasLockedFieldsAndCancelEscapeDoNotApprove()throws Exception {
        var draft=payments.createTuitionPlan("BANK_A"); plan(draft);
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        for(String language:new String[]{"en","vi"}) {
            page.getByTestId("language-"+language).click();
            page.getByTestId("approve-action").click();
            var dialog=page.getByTestId("payment-confirm-dialog");
            assertThat(dialog).isVisible();
            assertThat(page.getByTestId("confirm-total")).hasText("70,760,800 VND");
            assertThat(page.getByTestId("confirm-beneficiary")).hasText(CrossBorderService.SCHOOL_RECIPIENT_NAME);
            assertThat(page.getByTestId("confirm-source")).containsText("Bank A Everyday");
            assertThat(page.getByTestId("confirm-channel")).hasText("Bank A International Transfer");
            assertThat(page.getByTestId("confirm-bill")).containsText("SZDU-2026-MINH");
            assertThat(page.getByTestId("payment-confirm-submit")).isDisabled();
            assertThat(dialog.locator(".dialog-heading [data-payment-confirm-cancel]")).isFocused();
            assertThat(dialog).containsText(language.equals("vi")?"Không có tiền thật":"No real money");
            for(int[] viewport:new int[][]{{1366,768},{390,600},{360,430}}){
                page.setViewportSize(viewport[0],viewport[1]);
                for(String id:List.of("payment-confirm-check","payment-confirm-submit","payment-confirm-cancel","payment-confirm-stop")){
                    assertTrue((Boolean)page.getByTestId(id).evaluate("e=>{const r=e.getBoundingClientRect();return r.top>=0&&r.bottom<=innerHeight&&r.left>=0&&r.right<=innerWidth}"),id+" must fit "+viewport[0]+"x"+viewport[1]);
                }
                assertThat(dialog.locator("[data-confirm-quote-status]")).isInViewport();
                assertThat(page.getByTestId("confirm-total")).isInViewport();
                var body=dialog.locator(".confirmation-body");
                assertTrue((Boolean)body.evaluate("e=>e.scrollHeight>e.clientHeight"),"Details scroll without moving approval buttons");
                body.evaluate("e=>e.scrollTop=e.scrollHeight");
                assertTrue((Boolean)body.evaluate("e=>e.scrollTop>0"));
                body.evaluate("e=>e.scrollTop=0");
                capture("confirmation-"+viewport[0]+"-"+language);
            }
            page.setViewportSize(1366,900);
            for(int i=0;i<12;i++){page.keyboard().press("Tab");assertTrue((Boolean)dialog.evaluate("e=>e.contains(document.activeElement)"));}
            page.keyboard().press("Escape");
            assertThat(dialog).isHidden(); assertThat(page.getByTestId("approve-action")).isFocused();
            PaymentApprovalControls.cancel(page);
            assertThat(dialog).isHidden();
        }
        assertEquals(0,approvalRequests.size());assertEquals(0,payments.sandboxTransactionCount());
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
    }
    @Test void twoReviewsApproveSameLockedPlanOnlyOnce() {
        var draft=payments.createTuitionPlan("BANK_A");plan(draft);
        Page other=browser.newPage();other.setDefaultTimeout(10000);watch(other);
        other.navigate(BASE+"/?action="+draft.id()+"#agent-workspace");
        for(Page target:List.of(page,other)){
            target.getByTestId("approve-action").click();target.getByTestId("payment-confirm-check").check();
        }
        assertEquals(0,payments.sandboxTransactionCount());assertNull(payments.receiptForAction(draft.id()));
        page.getByTestId("payment-confirm-submit").click();
        assertThat(page.getByTestId("latest-action")).hasAttribute("data-status","COMPLETED");
        other.getByTestId("payment-confirm-submit").click();
        assertThat(other.getByTestId("latest-action")).hasAttribute("data-status","COMPLETED");
        String id=page.getByTestId("latest-receipt").getAttribute("data-transaction-id");
        assertThat(other.getByTestId("latest-receipt")).hasAttribute("data-transaction-id",id);
        assertEquals(2,approvalRequests.size());assertEquals(1,payments.sandboxTransactionCount());
        assertEquals(0,ledger.balance("PAYER_VND").compareTo(new BigDecimal("29239200")));
        assertThat(page.getByTestId("receipt-vnd-debit")).containsText("70,760,800");
        assertThat(page.getByTestId("receipt-credit")).containsText("20,000");
        page.reload();assertThat(page.getByTestId("payment-confirm-dialog")).hasCount(0);
        assertThat(page.getByTestId("latest-receipt")).hasAttribute("data-transaction-id",id);
    }
    @Test void expiryWhileDialogIsOpenDisablesConfirmationWithoutSubmitting() {
        plan(payments.createTuitionPlan("BANK_A"));
        page.clock().install();
        page.getByTestId("approve-action").click();page.getByTestId("payment-confirm-check").check();
        assertThat(page.getByTestId("payment-confirm-submit")).isEnabled();
        page.clock().fastForward(360000);
        assertThat(page.getByTestId("payment-confirm-submit")).isDisabled();
        assertThat(page.locator("[data-confirm-quote-status]")).containsText("Quote expired");
        page.getByTestId("payment-confirm-cancel").click();
        assertThat(page.getByTestId("refresh-expired-plan-quote")).isVisible();
        assertEquals(0,approvalRequests.size());assertEquals(0,payments.sandboxTransactionCount());
    }
    @Test void emergencyStopIsReachableInsideConfirmationAndBlocksThePlan() {
        page.setViewportSize(390,600);
        plan(payments.createTuitionPlan("BANK_A"));
        page.getByTestId("approve-action").click();page.getByTestId("payment-confirm-stop").click();
        assertThat(page.getByTestId("workspace-paused")).isVisible();idle();
        assertThat(page.getByTestId("payment-blocked")).isVisible();
        assertThat(page.getByTestId("payment-reason-code")).hasText("AGENT PAUSED");
        assertThat(page.locator(".reason-reference")).isVisible();
        assertThat(page.getByTestId("approve-action")).hasCount(0);
        assertEquals("PAUSED",payments.policy().state());
        assertEquals(0,approvalRequests.size());assertEquals(0,payments.sandboxTransactionCount());
    }
    @Test void mobileInternalTransferUsesCorrectLanguageAndFitsShortViewport()throws Exception {
        plan(payments.createLowRiskPlan(new BigDecimal("250000")));
        for(int width:new int[]{360,390})for(String language:new String[]{"en","vi"}){
            page.setViewportSize(width,600);page.getByTestId("language-"+language).click();
            page.getByTestId("approve-action").click();
            var dialog=page.getByTestId("payment-confirm-dialog");
            assertThat(dialog).containsText(language.equals("vi")?"Xác nhận chuyển nội bộ":"Confirm internal transfer");
            assertThat(dialog).not().containsText("FX markup");
            assertThat(page.getByTestId("confirm-total")).hasText("250,000 VND");
            assertThat(page.getByTestId("payment-confirm-check")).isVisible();
            page.getByTestId("payment-confirm-check").check();
            assertThat(page.getByTestId("payment-confirm-submit")).isEnabled();
            page.getByTestId("payment-confirm-submit").scrollIntoViewIfNeeded();
            assertThat(page.getByTestId("payment-confirm-submit")).isInViewport();
            fits(page);capture("confirmation-mobile-"+width+"-"+language);
            page.getByTestId("payment-confirm-cancel").click();
        }
        assertEquals(0,approvalRequests.size());assertEquals(0,payments.sandboxTransactionCount());
    }
    @Test void horizontalButtonsAndChannelSelectionAreReadOnly()throws Exception {
        for(int i=0;i<5;i++)GlobalAssistantFixtures.secondTuition(border);
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        page.navigate(BASE+"#student-finance");page.setViewportSize(390,844);
        for(String selector:new String[]{".student-bill-list",".channel-list"}){
            var track=page.locator(selector);
            var next=track.locator("xpath=preceding-sibling::*[1]").locator("button").last();
            assertThat(next).isVisible();assertThat(next).isEnabled();
            next.click();page.waitForFunction("selector=>document.querySelector(selector).scrollLeft>0",selector);
        }
        page.locator("[data-select-channel='VCB']").click();
        assertThat(page.getByTestId("channel-VCB")).hasClass(java.util.regex.Pattern.compile(".*selected-channel.*"));
        capture("comparison-mobile-390");
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
        assertEquals(0,payments.sandboxTransactionCount());
    }
}
