package com.example.finance;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;
import com.microsoft.playwright.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
 "server.port=8150","spring.datasource.url=jdbc:h2:mem:unified_browser;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
class UnifiedAccountsPlaywrightTest {
    @Autowired DemoDataService demo; @Autowired FinanceWorkspaceService workspace;
    @Autowired PhaseFourService payments; @MockitoBean LlmIntentClient model;
    Playwright playwright;Browser browser;Page page;
    @BeforeEach void open(){reset(model);when(model.enabled()).thenReturn(true);demo.resetAll();
        playwright=Playwright.create();browser=playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
        page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1440,1000));page.setDefaultTimeout(10000);page.onDialog(Dialog::accept);
    }
    @AfterEach void close(){if(browser!=null)browser.close();if(playwright!=null)playwright.close();}
    void navigate(){page.navigate("http://localhost:8150");}
    @Test void sameAccountInventoryAndOneChatAcrossAllViews(){
        workspace.addManualAccount("Travel cash","Cash","CASH",null,new BigDecimal("2000000"));navigate();
        assertThat(page.getByTestId("finance-chat")).hasCount(0);assertThat(page.locator("[data-assistant-panel] [data-chat-form]")).hasCount(1);
        assertThat(page.locator(".current-account-grid > article")).hasCount(7);
        assertThat(page.getByTestId("total-personal-balance")).containsText("317,000,000");
        page.locator("[data-dashboard-view=accounts]").click();assertThat(page.locator(".money-source-card")).hasCount(7);
        assertThat(page.locator(".money-source-grid")).containsText("Travel cash");
        when(model.classify(anyString())).thenReturn(new LlmIntent(LlmIntent.Intent.EXPLAIN_CURRENT_BALANCE,LlmIntent.ChannelPreference.NONE,new BigDecimal("0.95"),LlmIntent.ClarificationCode.NONE));
        page.getByTestId("assistant-launcher").click();page.getByTestId("assistant-conversation-input").fill("Total balances of all accounts");page.getByTestId("assistant-send-message").click();
        page.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')");
        assertThat(page.getByTestId("assistant-replies")).containsText("317000000.00");
        assertThat(page.getByTestId("assistant-replies")).containsText("Travel cash");
        assertEquals(0,payments.sandboxTransactionCount());
    }
    @Test void manyAccountsKeepFixedHeightAndScrollButtonDoesNotMovePageDesktopAndMobile(){
        for(int i=0;i<18;i++)workspace.addManualAccount("Cash "+i,"Cash","CASH",null,BigDecimal.TEN);navigate();
        page.locator("[data-dashboard-view=accounts]").click();var region=page.locator(".money-source-grid");
        assertEquals(480,((Number)region.evaluate("e=>e.clientHeight")).intValue());
        assertTrue((Boolean)region.evaluate("e=>e.scrollHeight>e.clientHeight"));
        var button=page.locator(".money-source-grid + .list-scroll-down");button.scrollIntoViewIfNeeded();
        double top=((Number)page.evaluate("()=>window.scrollY")).doubleValue();button.click();
        page.waitForFunction("()=>document.querySelector('.money-source-grid').scrollTop>100");
        assertEquals(top,((Number)page.evaluate("()=>window.scrollY")).doubleValue(),1);
        region.focus();page.keyboard().press("End");assertThat(region).isFocused();
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/unified-accounts-desktop.png")));
        page.setViewportSize(390,844);assertEquals(420,((Number)region.evaluate("e=>e.clientHeight")).intValue());
        assertTrue((Boolean)page.evaluate("()=>document.documentElement.scrollWidth<=window.innerWidth"));
        page.getByTestId("language-vi").click();assertThat(button).hasText("Cuộn xuống");
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/unified-accounts-mobile.png")));
        region.evaluate("e=>e.scrollTop=150");
        when(model.classify(anyString())).thenReturn(new LlmIntent(LlmIntent.Intent.EXPLAIN_CURRENT_BALANCE,LlmIntent.ChannelPreference.NONE,new BigDecimal("0.95"),LlmIntent.ClarificationCode.NONE));
        page.getByTestId("assistant-launcher").click();page.getByTestId("assistant-conversation-input").fill("Tổng số dư tất cả tài khoản");page.getByTestId("assistant-send-message").click();
        page.waitForFunction("()=>!document.querySelector('.content').hasAttribute('aria-busy')");page.getByTestId("assistant-close").click();
        assertEquals(150,((Number)page.locator(".money-source-grid").evaluate("e=>e.scrollTop")).intValue());

    }
    @Test void paymentUpdatesOverviewAccountsAndChatWithoutSecondDebit(){
        var plan=payments.createTuitionPlan("BANK_A");navigate();
        page.getByTestId("assistant-launcher").click();page.getByTestId("assistant-close").click();
        page.navigate("http://localhost:8150/?action="+plan.id()+"#agent-workspace");
        assertThat(page.getByTestId("latest-action")).hasAttribute("data-status","AWAITING_APPROVAL");
        assertThat(page.getByTestId("latest-receipt")).hasCount(0);
        PaymentApprovalControls.approve(page);page.waitForFunction("()=>!document.querySelector('.content').hasAttribute('aria-busy')");
        assertThat(page.getByTestId("latest-receipt")).containsText("29,239,200.00");
        page.getByTestId("audit-log").locator("summary").click();
        assertTrue(((Number)page.getByTestId("audit-log").locator(".table-scroll").evaluate("e=>e.clientHeight")).intValue()<=320);
        page.getByTestId("tab-transactions").click();
        page.getByTestId("transaction-view-history").click();
        assertFalse((Boolean)page.locator("#transactions .table-scroll").evaluate("e=>e.scrollHeight>e.clientHeight+2"));
        assertThat(page.locator("#transactions .table-scroll + .list-scroll-down")).hasCount(0);
        assertThat(page.locator("#transactions")).containsText("Bank A Everyday");
        page.getByTestId("tab-dashboard").click();assertThat(page.getByTestId("total-personal-balance")).containsText("244,239,200");
        assertThat(page.locator(".current-account-grid")).containsText("29,239,200");
        page.locator("[data-dashboard-view=accounts]").click();assertThat(page.getByTestId("money-source-CHECKING")).containsText("29,239,200");
        assertEquals(1,payments.sandboxTransactionCount());
    }
}
