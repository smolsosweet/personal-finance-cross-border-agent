package com.example.finance;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.LocatorAssertions;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Browser regressions for the contextual review -> approval -> receipt workflow. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
        "server.port=8095",
        "spring.datasource.url=jdbc:h2:mem:payment_workflow_e2e;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentWorkflowPlaywrightE2ETest {
    private static final String BASE_URL = "http://localhost:8095";
    @Autowired DemoDataService demoData;
    @Autowired CrossBorderService crossBorder;
    @Autowired PhaseFourService phaseFour;
    @Autowired JdbcTemplate db;
    private Playwright playwright;
    private Browser browser;

    @BeforeAll void setupBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel(System.getProperty("playwright.browser.channel", "chrome")).setHeadless(true));
    }

    @AfterAll void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach void resetSyntheticData() {
        demoData.resetAll();
    }

    @Test void mobileVietnameseAlipayReviewRemainsApprovalOnlyInDelegatedMode() {
        phaseFour.setMode("DELEGATED");
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(390, 844))) {
            Page page = context.newPage();
            var errors = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(BASE_URL);
            assertThat(page.locator(".mobile-tabs [data-tab='agent']")).isVisible();
            page.getByTestId("language-vi").click();
            page.locator(".mobile-tabs [data-tab='student']").click();
            page.getByTestId("plan-ALIPAY").click();

            assertThat(page.locator("#agent-workspace")).isVisible();
            assertThat(page.locator(".mobile-tabs [data-tab='agent']")).hasAttribute("aria-selected", "true");
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "AWAITING_APPROVAL");
            assertThat(page.getByTestId("payment-review")).containsText("SZDU-2026-MINH");
            assertThat(page.getByTestId("review-channel")).containsText("Alipay");
            assertThat(page.getByTestId("review-beneficiary")).containsText(CrossBorderService.SCHOOL_NAME);
            assertThat(page.getByTestId("review-source")).containsText("Alipay");
            assertThat(page.getByTestId("review-total")).containsText("71,032,100");
            assertThat(page.getByTestId("review-remaining")).containsText("3,967,900");
            assertThat(page.getByTestId("approve-action")).containsText("Phê duyệt");
            assertThat(page.getByTestId("approve-action")).isEnabled();
            assertThat(page.getByTestId("latest-receipt")).hasCount(0);
            assertEquals(0, phaseFour.sandboxTransactionCount(), "Delegated mode must not execute education payments");
            assertTrue(!(Boolean) page.getByTestId("payment-demo-tools").evaluate("element => element.open"));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1"),
                    "The mobile payment review must fit the viewport");
            dismissNotices(page);
            page.screenshot(new Page.ScreenshotOptions().setFullPage(true)
                    .setPath(Path.of("target", "payment-review-mobile-vi.png")));
            assertTrue(errors.isEmpty(), String.join(" | ", errors));
        }
    }

    @Test void approvingExactPlanShowsItsReceiptAndCompletedSnapshotSurvivesNewQuotes() {
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1000))) {
            Page page = context.newPage();
            var errors = new ArrayList<String>();
            var approvals = new ArrayList<String>();
            var documentNavigations = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(BASE_URL);
            page.getByTestId("tab-student").click();
            page.onRequest(request -> {
                if ("POST".equals(request.method()) && request.url().endsWith("/approve")) approvals.add(request.url());
                if (request.isNavigationRequest() && request.frame().parentFrame() == null) documentNavigations.add(request.url());
            });
            page.evaluate("() => { window.__paymentReviewDocument = 'unchanged'; }");
            page.getByTestId("plan-BANK_A").click();
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "AWAITING_APPROVAL");
            String actionId = page.getByTestId("latest-action").getAttribute("data-action-id");
            String channelSnapshot = page.getByTestId("review-channel").innerText();
            String totalSnapshot = page.getByTestId("review-total").innerText();
            assertThat(page.getByTestId("review-channel")).containsText("Bank A");
            assertThat(page.getByTestId("review-source")).containsText("Bank A");
            assertThat(page.getByTestId("review-total")).containsText("70,760,800");
            assertThat(page.getByTestId("latest-receipt")).hasCount(0);
            dismissNotices(page);
            page.screenshot(new Page.ScreenshotOptions().setFullPage(true)
                    .setPath(Path.of("target", "payment-review-desktop.png")));

            page.onceDialog(dialog -> dialog.dismiss());
            page.getByTestId("approve-action").click();
            assertEquals(0, approvals.size(), "Dismissing confirmation must not submit approval");
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "AWAITING_APPROVAL");

            page.onceDialog(dialog -> dialog.accept());
            page.getByTestId("approve-action").click();
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "COMPLETED");
            assertThat(page.getByTestId("latest-receipt")).hasAttribute("data-action-id", actionId);
            assertThat(page.getByTestId("receipt-vnd-debit")).containsText("70,760,800.00 VND");
            assertThat(page.getByTestId("receipt-credit")).containsText("20,000.00 CNY");
            String transactionId = page.getByTestId("latest-receipt").getAttribute("data-transaction-id");
            dismissNotices(page);
            page.screenshot(new Page.ScreenshotOptions().setFullPage(true)
                    .setPath(Path.of("target", "payment-receipt-desktop.png")));
            assertEquals(1, approvals.size());
            assertEquals(1, phaseFour.sandboxTransactionCount());
            assertEquals("unchanged", page.evaluate("() => window.__paymentReviewDocument"));
            assertTrue(documentNavigations.isEmpty(), "Creating and approving must update in place: " + documentNavigations);

            page.getByTestId("return-to-comparison").click();
            page.getByTestId("refresh-quotes").click();
            waitForUpdate(page);
            page.navigate(BASE_URL + "/?action=" + actionId + "#agent-workspace");
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "COMPLETED");
            assertThat(page.getByTestId("review-channel")).hasText(channelSnapshot);
            assertThat(page.getByTestId("review-total")).hasText(totalSnapshot);
            assertThat(page.getByTestId("latest-receipt")).hasAttribute("data-action-id", actionId);
            assertThat(page.getByTestId("latest-receipt")).hasAttribute("data-transaction-id", transactionId);
            assertThat(page.getByTestId("approve-action")).hasCount(0);
            assertThat(page.getByTestId("cancel-action")).hasCount(0);
            assertTrue(errors.isEmpty(), String.join(" | ", errors));
        }
    }

    @Test void nextBillPlanAndCancellationNeverDisplayAnotherActionsReceipt() {
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1000))) {
            Page page = context.newPage();
            var errors = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(BASE_URL);
            page.getByTestId("tab-student").click();
            page.getByTestId("plan-BANK_A").click();
            String completedAction = page.getByTestId("latest-action").getAttribute("data-action-id");
            page.onceDialog(dialog -> dialog.accept());
            page.getByTestId("approve-action").click();
            assertThat(page.getByTestId("latest-receipt")).hasAttribute("data-action-id", completedAction);
            String completedTransaction = page.getByTestId("latest-receipt").getAttribute("data-transaction-id");

            int secondBill = addVerifiedBill("Campus insurance workflow", "1200.00");
            page.navigate(BASE_URL + "/?fixtureBill=" + secondBill + "#student-finance");
            assertThat(page.getByTestId("expense-" + secondBill)).containsText("Campus insurance workflow");
            page.getByTestId("plan-BANK_A").click();
            assertThat(page.getByTestId("review-bill")).containsText("Campus insurance workflow");
            String pendingAction = page.getByTestId("latest-action").getAttribute("data-action-id");
            assertNotEquals(completedAction, pendingAction);
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "AWAITING_APPROVAL");
            assertThat(page.getByTestId("review-bill")).containsText("Campus insurance workflow");
            assertThat(page.getByTestId("latest-receipt")).hasCount(0);
            assertEquals(1, phaseFour.sandboxTransactionCount());

            page.onceDialog(dialog -> dialog.accept());
            page.getByTestId("cancel-action").click();
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "CANCELED");
            assertThat(page.getByTestId("approve-action")).hasCount(0);
            assertThat(page.getByTestId("latest-receipt")).hasCount(0);
            assertEquals(1, phaseFour.sandboxTransactionCount());
            page.getByTestId("return-to-comparison").click();
            page.getByTestId("plan-BANK_A").click();
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "AWAITING_APPROVAL");
            String newerPending = page.getByTestId("latest-action").getAttribute("data-action-id");
            assertNotEquals(pendingAction, newerPending);
            assertThat(page.getByTestId("latest-receipt")).hasCount(0);
            page.getByTestId("payment-history").locator("summary").click();
            var previousPlan = page.getByTestId("payment-history")
                    .locator("[data-open-payment-plan][data-action-id='" + completedAction + "']");
            previousPlan.click();
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-action-id", completedAction);
            assertThat(page.getByTestId("latest-receipt")).hasAttribute("data-transaction-id", completedTransaction);
            assertThat(page.getByTestId("latest-receipt")).hasAttribute("data-action-id", completedAction);
            page.getByTestId("return-to-comparison").click();
            page.getByTestId("refresh-quotes").click();
            waitForUpdate(page);
            page.getByTestId("tab-agent").click();
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-action-id", completedAction);
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "COMPLETED");
            assertThat(page.getByTestId("latest-receipt")).hasAttribute("data-transaction-id", completedTransaction);
            assertTrue(errors.isEmpty(), String.join(" | ", errors));
        }
    }

    @Test void storedQuoteExpiresLiveAndDisablesApprovalWithoutPageReload() {
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1000))) {
            Page page = context.newPage();
            var errors = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(BASE_URL);
            page.getByTestId("tab-student").click();
            db.update("UPDATE fx_quotes SET expires_at=? WHERE channel_id='BANK_A'", LocalDateTime.now().plusSeconds(7));
            page.getByTestId("plan-BANK_A").click();
            assertThat(page.getByTestId("approve-action")).isEnabled();
            long expiry = Long.parseLong(page.locator("[data-plan-expiry]").getAttribute("data-plan-expiry"));
            assertTrue(expiry > System.currentTimeMillis(), "The review must start with an unexpired stored quote");
            page.evaluate("() => { window.__quoteReviewDocument = 'expiry-without-reload'; }");
            assertThat(page.getByTestId("approve-action")).isDisabled(new LocatorAssertions.IsDisabledOptions().setTimeout(12000));
            assertThat(page.getByTestId("plan-expiry-status")).containsText("expired");
            assertThat(page.getByTestId("return-to-comparison")).isVisible();
            assertEquals("expiry-without-reload", page.evaluate("() => window.__quoteReviewDocument"));
            assertEquals(0, phaseFour.sandboxTransactionCount());
            assertThat(page.getByTestId("latest-receipt")).hasCount(0);
            assertTrue(errors.isEmpty(), String.join(" | ", errors));
        }
    }

    @Test void emergencyStopIsAccessibleWithDemoToolsClosedAndBlocksPaymentUntilRecovery() {
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1000))) {
            Page page = context.newPage();
            var errors = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(BASE_URL);
            page.getByTestId("tab-student").click();
            page.getByTestId("plan-BANK_A").click();
            String actionId = page.getByTestId("latest-action").getAttribute("data-action-id");
            assertTrue(!(Boolean) page.getByTestId("payment-demo-tools").evaluate("element => element.open"));
            assertThat(page.getByTestId("emergency-stop")).isVisible();
            assertEquals(0, page.getByTestId("payment-demo-tools").getByTestId("emergency-stop").count(),
                    "Emergency Stop must remain outside collapsed demo tools");
            page.getByTestId("emergency-stop").click();
            assertThat(page.getByTestId("agent-state")).containsText("Stopped");
            assertThat(page.getByTestId("approve-action")).hasCount(0);
            assertEquals(0, phaseFour.sandboxTransactionCount());
            assertThat(page.getByTestId("latest-receipt")).hasCount(0);

            page.getByTestId("emergency-resume").click();
            assertThat(page.getByTestId("agent-state")).containsText("Active");
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-action-id", actionId);
            assertThat(page.getByTestId("approve-action")).isEnabled();
            page.onceDialog(dialog -> dialog.accept());
            page.getByTestId("approve-action").click();
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "COMPLETED");
            assertEquals(1, phaseFour.sandboxTransactionCount());
            assertTrue(errors.isEmpty(), String.join(" | ", errors));
        }
    }

    private void dismissNotices(Page page) {
        var dismiss = page.locator(".notice .notice-dismiss");
        while (dismiss.count() > 0) dismiss.first().click();
    }
    private void waitForUpdate(Page page) {
        page.waitForFunction("() => document.querySelector('.content')?.getAttribute('aria-busy') !== 'true'");
    }

    private int addVerifiedBill(String title, String amount) {
        return crossBorder.addExpense("INSURANCE", title, CrossBorderService.SCHOOL_NAME,
                new BigDecimal(amount), "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT_NAME,
                CrossBorderService.SCHOOL_RECIPIENT_BANK, CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE,
                CrossBorderService.SCHOOL_RECIPIENT, "REF-" + title.replace(' ', '-'),
                LocalDate.now().plusDays(30), null, null, null);
    }
}
