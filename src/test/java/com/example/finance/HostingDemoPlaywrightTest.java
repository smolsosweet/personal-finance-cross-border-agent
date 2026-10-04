package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Real browser/app, hosting profile; AI disabled, zero external model calls. */
@ActiveProfiles("hosting")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties={"server.port=8124","finbridge.llm.enabled=false"})
class HostingDemoPlaywrightTest {
    @Autowired DemoDataService demo;
    @Autowired PhaseFourService payments;
    @Autowired JdbcTemplate db;
    @Autowired Environment environment;
    @BeforeEach void reset(){demo.resetAll();}

    @Test void hostingUsesEphemeralDatabaseAndShowsBilingualSharedNotice(){
        assertTrue(environment.getProperty("spring.datasource.url").startsWith("jdbc:h2:mem:"));
        assertEquals("gemini",environment.getProperty("finbridge.llm.provider"));
        assertEquals("10s",environment.getProperty("finbridge.llm.connect-timeout"));
        assertEquals("30s",environment.getProperty("finbridge.llm.request-timeout"));
        try(Playwright pw=Playwright.create();Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true))){
            Page page=browser.newPage();page.navigate("http://localhost:8124");
            page.getByTestId("language-en").click();
            assertThat(page.getByTestId("shared-demo-notice")).containsText("Reset affects everyone. Restart restores the seed data.");
            assertThat(page.getByTestId("reset-demo").locator("..")).hasAttribute("data-confirm-en","Reset shared synthetic data for the whole team? This affects everyone.");
            page.getByTestId("language-vi").click();
            assertThat(page.getByTestId("shared-demo-notice")).containsText("Reset ảnh hưởng mọi người. Restart đưa dữ liệu về seed.");
        }
    }

    @Test void teamSeesSharedDraftAndResetButHasSeparateChatSessions(){
        try(Playwright pw=Playwright.create();Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext first=browser.newContext();BrowserContext second=browser.newContext()){
            Page a=first.newPage(),b=second.newPage();a.navigate("http://localhost:8124");b.navigate("http://localhost:8124");
            var draft=payments.createTuitionPlan("BANK_A");
            a.reload();b.reload();
            assertThat(a.getByTestId("tab-agent")).isVisible();assertThat(b.getByTestId("tab-agent")).isVisible();
            assertEquals("AWAITING_APPROVAL",payments.action(draft.id()).status());assertNull(payments.receiptForAction(draft.id()));
            a.getByTestId("assistant-launcher").click();a.getByTestId("assistant-conversation-input").fill("Show my budgets");a.getByTestId("assistant-send-message").click();
            a.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')");
            b.getByTestId("assistant-launcher").click();assertThat(b.getByTestId("assistant-replies")).not().containsText("Show my budgets");
            a.getByTestId("assistant-close").click();a.getByTestId("tab-dashboard").click();a.onDialog(Dialog::accept);a.getByTestId("reset-demo").click();
            a.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')");b.reload();
            assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM action_plans",Integer.class));
            assertThat(b.getByTestId("tab-agent")).isHidden();assertEquals(0,payments.sandboxTransactionCount());
        }
    }
}
