package com.example.finance;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Opt-in browser smoke using real qwen3:4b; no model mock and no payment approval. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
    "server.port=8102",
    "spring.datasource.url=jdbc:h2:mem:assistant_panel_live;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "app.demo-tools-enabled=false", "finbridge.llm.enabled=true", "finbridge.llm.provider=ollama",
    "finbridge.llm.model=qwen3:4b", "finbridge.llm.base-url=http://localhost:11434", "finbridge.llm.request-timeout=60s"
})
class AssistantPanelOllamaLiveIT {
    @Autowired DemoDataService demo;
    @Autowired PhaseFourService payments;
    @Autowired JdbcTemplate db;

    @Test void realModelAnswersVietnameseBudgetThenEnglishDraftStillNeedsApproval() {
        demo.resetAll();
        var errors = new ArrayList<String>();
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true))) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 1000));
            page.setDefaultTimeout(90000);
            page.onPageError(errors::add);
            page.navigate("http://localhost:8102");
            page.getByTestId("language-vi").click();
            page.getByTestId("tab-transactions").click();
            page.getByTestId("assistant-launcher").click();
            page.getByTestId("assistant-question-budget").click();
            var before = PersonalFinanceAiIntegrationTest.snapshot(db);
            long start = System.nanoTime();
            page.getByTestId("assistant-send-message").click();
            assertThat(page.getByTestId("assistant-panel").locator("[data-chat-processing]")).isVisible();
            idle(page);
            long budgetMillis = (System.nanoTime() - start) / 1_000_000;
            assertEquals("EXPLAIN_BUDGET_STATUS", latestIntent());
            assertThat(page.getByTestId("assistant-replies")).containsText("Nguồn: ngân sách cấu hình");
            assertThat(page.getByTestId("assistant-screen")).hasText("Giao dịch");
            assertEquals(before, PersonalFinanceAiIntegrationTest.snapshot(db));
            assertNull(payments.latestAction());
            System.out.printf("ASSISTANT_LIVE budget_ms=%d intent=EXPLAIN_BUDGET_STATUS financial_unchanged=true%n", budgetMillis);
            page.getByTestId("assistant-close").click();
            page.getByTestId("tab-student").click();
            page.getByTestId("assistant-launcher").click();
            page.getByTestId("assistant-language-en").click();
            page.getByTestId("assistant-conversation-input").fill("Prepare the cheapest tuition payment draft.");
            start = System.nanoTime();
            page.getByTestId("assistant-send-message").click();
            idle(page);
            long draftMillis = (System.nanoTime() - start) / 1_000_000;
            assertEquals("CREATE_TUITION_PLAN", latestIntent());
            var plan = payments.latestAction();
            assertNotNull(plan);
            assertEquals("AWAITING_APPROVAL", plan.status());
            assertEquals("APPROVAL", plan.requiredPermission());
            assertEquals(0, payments.sandboxTransactionCount());
            assertNull(payments.receiptForAction(plan.id()));
            assertThat(page.getByTestId("assistant-review-plan")).isVisible();
            page.getByTestId("assistant-review-plan").click();
            assertThat(page.getByTestId("assistant-panel")).isHidden();
            assertThat(page.getByTestId("approve-action")).isVisible();
            page.getByTestId("assistant-plan-help").click();
            assertThat(page.getByTestId("assistant-plan-total")).hasText(page.getByTestId("review-total").innerText());
            page.getByTestId("assistant-panel").screenshot(new Locator.ScreenshotOptions().setPath(Path.of("target/assistant-ollama-live.png")));
            System.out.printf("ASSISTANT_LIVE draft_ms=%d intent=CREATE_TUITION_PLAN status=AWAITING_APPROVAL payments=0 receipts=0%n", draftMillis);
            assertTrue(errors.isEmpty(), String.join(" | ", errors));
        }
    }

    private void idle(Page page) { page.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')"); }
    private String latestIntent() {
        return db.queryForObject("SELECT reason_code FROM audit_log WHERE event_type='INTENT_CLASSIFIED' ORDER BY occurred_at DESC FETCH FIRST 1 ROW ONLY", String.class);
    }
}
