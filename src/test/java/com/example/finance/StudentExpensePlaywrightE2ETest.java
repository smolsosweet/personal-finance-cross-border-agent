package com.example.finance;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT, properties={
        "server.port=8096",
        "spring.datasource.url=jdbc:h2:mem:student_expense_e2e;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudentExpensePlaywrightE2ETest {
    private static final String BASE_URL="http://localhost:8096";
    @Autowired DemoDataService demoData;
    @Autowired CrossBorderService crossBorder;
    @Autowired JdbcTemplate db;
    private Playwright playwright;
    private Browser browser;

    @BeforeAll void setup() {
        playwright=Playwright.create();
        browser=playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel(System.getProperty("playwright.browser.channel","chrome")).setHeadless(true));
    }

    @AfterAll void close() {
        if(browser!=null) browser.close();
        if(playwright!=null) playwright.close();
    }

    @BeforeEach void resetSyntheticData() {
        demoData.resetAll();
    }

    @Test void billCardsSelectByPointerAndKeyboardWithoutNavigatingOrTriggeringNestedActions() {
        int otherBill = addVerifiedBill("Campus insurance", "1200.00");
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1000))) {
            Page page = context.newPage();
            var errors = new ArrayList<String>();
            var navigations = new ArrayList<String>();
            var selections = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(BASE_URL);
            page.getByTestId("tab-student").click();
            page.onFrameNavigated(frame -> {
                if (frame.parentFrame() == null) navigations.add(frame.url());
            });
            page.onRequest(request -> {
                if ("POST".equals(request.method()) && request.url().endsWith("/student/expenses/select")) {
                    selections.add(request.postData());
                }
            });

            page.getByTestId("expense-1").click();
            assertThat(page.getByTestId("expense-1")).hasClass(java.util.regex.Pattern.compile(".*selected.*"));
            assertThat(page.getByTestId("tuition-bill")).containsText("20,000");
            assertEquals(1, crossBorder.selectedExpense().id());
            page.getByTestId("expense-1").click();

            var otherSelect = page.getByTestId("expense-" + otherBill).locator(".student-bill-select");
            otherSelect.focus();
            otherSelect.press("Enter");
            assertThat(page.getByTestId("expense-" + otherBill))
                    .hasClass(java.util.regex.Pattern.compile(".*selected.*"));
            assertThat(page.getByTestId("tuition-bill")).containsText("1,200");

            var originalSelect = page.getByTestId("expense-1").locator(".student-bill-select");
            originalSelect.focus();
            originalSelect.press("Space");
            assertThat(page.getByTestId("expense-1")).hasClass(java.util.regex.Pattern.compile(".*selected.*"));
            page.getByTestId("expense-" + otherBill).getByRole(AriaRole.BUTTON,
                    new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Edit").setExact(true)).click();
            assertThat(page.locator("#edit-student-expense-" + otherBill)).isVisible();
            assertThat(page.getByTestId("expense-1")).hasClass(java.util.regex.Pattern.compile(".*selected.*"));
            page.locator("#edit-student-expense-" + otherBill + " [data-close-dialog]").click();

            assertEquals(3, selections.size(), "Selecting the current card or clicking Edit must not submit selection");
            assertEquals(1, crossBorder.selectedExpense().id());
            assertTrue(navigations.isEmpty(), "Bill selection must not reload the main document: " + navigations);
            assertTrue(errors.isEmpty(), String.join(" | ", errors));
        }
    }

    @Test void selectingBillHighlightsInPlaceWithoutReorderingAnySortOrClearingFilters() {
        int first = addVerifiedBill("Campus stable Alpha", "1200.00");
        int second = addVerifiedBill("Campus stable Beta", "1200.00");
        int third = addVerifiedBill("Campus stable Gamma", "1200.00");
        db.update("UPDATE international_bills SET created_at=TIMESTAMP '2026-01-01 09:00:00' WHERE id IN (?,?,?)",
                first, second, third);
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1000))) {
            Page page = context.newPage();
            var errors = new ArrayList<String>();
            var navigations = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(BASE_URL);
            page.getByTestId("tab-student").click();
            page.onFrameNavigated(frame -> {
                if (frame.parentFrame() == null) navigations.add(frame.url());
            });
            page.getByTestId("student-bill-search").fill("Campus stable");
            page.getByTestId("student-bill-status-filter").selectOption("ACTIVE");
            page.getByTestId("student-bill-verification-filter").selectOption("VERIFIED");

            for (String sort : List.of("due-asc", "newest", "amount-desc", "provider-asc")) {
                page.getByTestId("student-bill-sort").selectOption(sort);
                var rows = page.locator("[data-bill-row]:visible");
                assertEquals(3, rows.count());
                List<?> visibleOrder = (List<?>) rows.evaluateAll("rows => rows.map(row => row.dataset.billRow)");
                List<String> expectedOrder = "newest".equals(sort)
                        ? List.of(String.valueOf(third), String.valueOf(second), String.valueOf(first))
                        : List.of(String.valueOf(first), String.valueOf(second), String.valueOf(third));
                assertEquals(expectedOrder, visibleOrder, "Tied " + sort + " values must use stable bill IDs");
                List<?> before = (List<?>) page.locator("[data-bill-row]")
                        .evaluateAll("rows => rows.map(row => row.dataset.billRow)");
                String targetId = (String) rows.evaluateAll("rows => rows.slice(1).find(row => !row.classList.contains('selected')).dataset.billRow");
                var target = page.getByTestId("expense-" + targetId);
                String targetTitle = target.locator(".student-bill-identity > strong").innerText();

                target.click();
                assertThat(page.getByTestId("expense-" + targetId))
                        .hasClass(java.util.regex.Pattern.compile(".*selected.*"));
                assertThat(page.getByTestId("tuition-bill")).containsText(targetTitle);
                page.waitForFunction("() => document.querySelector('.content')?.getAttribute('aria-busy') !== 'true'");

                assertEquals(before, page.locator("[data-bill-row]")
                        .evaluateAll("rows => rows.map(row => row.dataset.billRow)"),
                        "Selection must not move a card to the beginning under " + sort);
                assertEquals(visibleOrder, page.locator("[data-bill-row]:visible")
                        .evaluateAll("rows => rows.map(row => row.dataset.billRow)"));
                assertEquals(1, page.locator("[data-bill-row].selected").count());
                assertEquals(Integer.parseInt(targetId), crossBorder.selectedExpense().id());
                assertEquals("Campus stable", page.getByTestId("student-bill-search").inputValue());
                assertEquals("ACTIVE", page.getByTestId("student-bill-status-filter").inputValue());
                assertEquals("VERIFIED", page.getByTestId("student-bill-verification-filter").inputValue());
                assertEquals(sort, page.getByTestId("student-bill-sort").inputValue());
                assertThat(page.getByTestId("student-bill-page")).hasText("Page 1 of 1");
            }
            assertTrue(navigations.isEmpty(), "Selection must update without document navigation: " + navigations);
            assertTrue(errors.isEmpty(), String.join(" | ", errors));
        }
    }

    @Test void tuitionSelectionPreservesMobileScrollAndSuccessNoticeCanAutoDismissOrClose() {
        int otherBill = addVerifiedBill("Campus selection fixture", "1200.00");
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(390, 844))) {
            Page page = context.newPage();
            var errors = new ArrayList<String>();
            var navigations = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(BASE_URL);
            page.locator(".mobile-tabs [data-tab='student']").click();
            assertEquals(otherBill, crossBorder.selectedExpense().id());
            page.onFrameNavigated(frame -> {
                if (frame.parentFrame() == null) navigations.add(frame.url());
            });

            var tuitionSelect = page.getByTestId("expense-1").locator(".student-bill-select");
            tuitionSelect.scrollIntoViewIfNeeded();
            page.evaluate("""
                    () => {
                        const bill = document.querySelector('[data-testid="expense-1"]');
                        window.scrollTo(0, Math.max(150, bill.getBoundingClientRect().top + window.scrollY - 140));
                        document.querySelector('[data-testid="student-expense-list"]').scrollLeft = 20;
                    }
                    """);
            page.waitForFunction("() => window.scrollY >= 150 && document.querySelector('[data-testid=student-expense-list]').scrollLeft > 0");
            double previousY = ((Number) page.evaluate("() => window.scrollY")).doubleValue();
            double previousX = ((Number) page.getByTestId("student-expense-list")
                    .evaluate("element => element.scrollLeft")).doubleValue();
            page.evaluate("""
                    () => {
                        window.__billSelectionScroll = [window.scrollY];
                        window.addEventListener('scroll', () => window.__billSelectionScroll.push(window.scrollY), { passive: true });
                    }
                    """);

            tuitionSelect.click();
            assertThat(page.getByTestId("expense-1")).hasClass(java.util.regex.Pattern.compile(".*selected.*"));
            assertEquals(1, crossBorder.selectedExpense().id());
            page.waitForFunction("() => document.querySelector('.content')?.getAttribute('aria-busy') !== 'true'");
            page.waitForFunction("value => Math.abs(window.scrollY - value) <= 2", previousY);
            assertEquals(previousX, ((Number) page.getByTestId("student-expense-list")
                    .evaluate("element => element.scrollLeft")).doubleValue(), 2);
            assertTrue((Boolean) page.evaluate("() => window.__billSelectionScroll.every(value => value >= 150)"),
                    "Selecting tuition must never jump to the top before restoring the previous scroll");
            assertThat(page.locator(".notice[role='status']")).hasCount(1);
            assertThat(page.locator(".notice[role='status'] .notice-dismiss[data-dismiss-notice]")).isVisible();
            assertThat(page.locator(".notice[role='status']")).hasCount(0,
                    new com.microsoft.playwright.assertions.LocatorAssertions.HasCountOptions().setTimeout(6000));

            page.getByTestId("expense-" + otherBill).locator(".student-bill-select-form")
                    .evaluate("form => form.requestSubmit()");
            assertThat(page.locator(".notice[role='status']")).hasCount(1);
            page.locator(".notice[role='status'] .notice-dismiss").click();
            assertThat(page.locator(".notice[role='status']")).hasCount(0);
            assertTrue(navigations.isEmpty(), "Selecting bills must update in place: " + navigations);
            assertTrue(errors.isEmpty(), String.join(" | ", errors));
        }
    }

    @Test void replacementNoticeHasItsOwnLifetimeAndRequestErrorsRemainUntilDismissed() {
        int otherBill = addVerifiedBill("Campus notice fixture", "1200.00");
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1000))) {
            Page page = context.newPage();
            var errors = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(BASE_URL);
            page.getByTestId("tab-student").click();
            page.getByTestId("expense-1").locator(".student-bill-select-form")
                    .evaluate("form => form.requestSubmit()");
            assertThat(page.locator(".notice[role='status']")).hasCount(1);
            page.evaluate("() => { window.__firstNoticeTime = performance.now(); }");
            page.waitForFunction("() => performance.now() - window.__firstNoticeTime >= 2200");

            page.getByTestId("expense-" + otherBill).locator(".student-bill-select-form")
                    .evaluate("form => form.requestSubmit()");
            assertThat(page.getByTestId("expense-" + otherBill)).hasClass(java.util.regex.Pattern.compile(".*selected.*"));
            assertThat(page.locator(".notice[role='status']")).hasCount(1);
            page.evaluate("() => { window.__replacementNotice = document.querySelector('.notice[role=status]'); }");
            page.waitForFunction("() => performance.now() - window.__firstNoticeTime >= 4400");
            assertThat(page.locator(".notice[role='status']")).hasCount(1);
            assertTrue((Boolean) page.evaluate("() => document.querySelector('.notice[role=status]') === window.__replacementNotice"),
                    "An old success timer must not dismiss the newer notice");
            assertThat(page.locator(".notice[role='status']")).hasCount(0,
                    new com.microsoft.playwright.assertions.LocatorAssertions.HasCountOptions().setTimeout(5000));

            page.route("**/student/quotes/refresh", route -> route.abort());
            page.getByTestId("refresh-quotes").click();
            assertThat(page.locator(".notice.request-error[role='alert']")).hasCount(1);
            page.evaluate("() => { window.__errorNoticeTime = performance.now(); }");
            page.waitForFunction("() => performance.now() - window.__errorNoticeTime >= 4400");
            assertThat(page.locator(".notice.request-error[role='alert']")).hasCount(1);
            page.locator(".notice.request-error .notice-dismiss[data-dismiss-notice]").click();
            assertThat(page.locator(".notice.request-error[role='alert']")).hasCount(0);
            assertTrue(errors.isEmpty(), String.join(" | ", errors));
        }
    }
    @Test void quoteRefreshPreservesMobileScrollFiltersAndPagesAndRestartsLiveExpiry() {
        for (int index = 1; index <= 7; index++) {
            addVerifiedBill("Campus fee " + index, String.valueOf(1000 + index));
        }
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(390, 844))) {
            Page page = context.newPage();
            var errors = new ArrayList<String>();
            var navigations = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(BASE_URL);
            page.locator(".mobile-tabs [data-tab='student']").click();
            page.onFrameNavigated(frame -> {
                if (frame.parentFrame() == null) navigations.add(frame.url());
            });
            page.getByTestId("student-bill-search").fill("Campus");
            page.getByTestId("student-bill-status-filter").selectOption("ACTIVE");
            page.getByTestId("student-bill-sort").selectOption("amount-desc");
            page.getByTestId("student-bill-next").click();
            assertThat(page.getByTestId("student-bill-page")).hasText("Page 2 of 2");
            page.getByTestId("refresh-quotes").scrollIntoViewIfNeeded();
            page.getByTestId("student-expense-list").evaluate("element => { element.scrollLeft = element.scrollWidth; }");
            page.getByTestId("payment-account-list").evaluate("element => { element.scrollLeft = element.scrollWidth; }");
            page.waitForFunction("""
                    () => document.querySelector('[data-testid="student-expense-list"]').scrollLeft > 0
                            && document.querySelector('[data-testid="payment-account-list"]').scrollLeft > 0
                    """);
            double previousY = ((Number) page.evaluate("() => window.scrollY")).doubleValue();
            double previousBillX = ((Number) page.getByTestId("student-expense-list")
                    .evaluate("element => element.scrollLeft")).doubleValue();
            double previousChannelX = ((Number) page.getByTestId("payment-account-list")
                    .evaluate("element => element.scrollLeft")).doubleValue();
            String previousExpiry = page.getByTestId("channel-BANK_A").getAttribute("data-quote-expiry");
            page.getByTestId("refresh-quotes").click();
            assertThat(page.locator(".notice")).containsText("Synthetic FX quotes refreshed");
            page.waitForFunction("() => document.querySelector('.content')?.getAttribute('aria-busy') !== 'true'");
            assertEquals("Campus", page.getByTestId("student-bill-search").inputValue());
            assertEquals("ACTIVE", page.getByTestId("student-bill-status-filter").inputValue());
            assertEquals("amount-desc", page.getByTestId("student-bill-sort").inputValue());
            assertThat(page.getByTestId("student-bill-page")).hasText("Page 2 of 2");
            assertEquals(2, page.locator("[data-bill-row]:visible").count());
            page.waitForFunction("value => Math.abs(window.scrollY - value) <= 2", previousY);
            assertEquals(previousBillX, ((Number) page.getByTestId("student-expense-list")
                    .evaluate("element => element.scrollLeft")).doubleValue(), 2);
            assertEquals(previousChannelX, ((Number) page.getByTestId("payment-account-list")
                    .evaluate("element => element.scrollLeft")).doubleValue(), 2);
            assertThat(page.getByTestId("tab-student")).hasAttribute("aria-selected", "true");
            assertTrue(previousBillX > 0 && previousChannelX > 0, "Fixture must exercise both horizontal lists");
            assertTrue(Long.parseLong(page.getByTestId("channel-BANK_A").getAttribute("data-quote-expiry"))
                    >= Long.parseLong(previousExpiry));

            page.getByTestId("channel-BANK_A").evaluate("card => { card.dataset.quoteExpiry = String(Date.now() + 1200); }");
            page.waitForFunction("""
                    () => document.querySelector('[data-testid="channel-BANK_A"] [data-quote-live-status]')
                            .textContent.includes('Quote expired')
                    """);
            assertThat(page.getByTestId("channel-BANK_A").locator(".planning-form")).isHidden();
            assertTrue(navigations.isEmpty(), "Quote refresh must update the document in place: " + navigations);
            assertTrue(errors.isEmpty(), String.join(" | ", errors));
        }
    }

    @Test void resetHonorsConfirmationAndKeepsTabWhileOfflineRefreshDoesNotRetry() {
        int otherBill = addVerifiedBill("Campus reset fixture", "900.00");
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 800))) {
            Page page = context.newPage();
            var errors = new ArrayList<String>();
            var navigations = new ArrayList<String>();
            var resetRequests = new ArrayList<String>();
            var refreshRequests = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(BASE_URL);
            page.getByTestId("tab-student").click();
            page.onFrameNavigated(frame -> {
                if (frame.parentFrame() == null) navigations.add(frame.url());
            });
            page.onRequest(request -> {
                if (!"POST".equals(request.method())) return;
                if (request.url().endsWith("/reset")) resetRequests.add(request.url());
                if (request.url().endsWith("/student/quotes/refresh")) refreshRequests.add(request.url());
            });
            page.getByTestId("student-bill-search").fill("Campus");
            page.getByTestId("student-bill-sort").selectOption("amount-desc");
            page.evaluate("() => window.scrollTo(0, 150)");
            double previousY = ((Number) page.evaluate("() => window.scrollY")).doubleValue();
            page.onceDialog(confirm -> confirm.dismiss());
            page.locator("form[action='/reset']").evaluate("form => form.requestSubmit()");
            assertEquals(0, resetRequests.size());
            assertEquals(otherBill, crossBorder.selectedExpense().id());
            assertEquals("Campus", page.getByTestId("student-bill-search").inputValue());

            page.onceDialog(confirm -> confirm.accept());
            page.locator("form[action='/reset']").evaluate("form => form.requestSubmit()");
            assertThat(page.getByTestId("expense-" + otherBill)).hasCount(0);
            assertThat(page.getByTestId("expense-1")).hasClass(java.util.regex.Pattern.compile(".*selected.*"));
            assertEquals(1, resetRequests.size(), "Confirmation should produce exactly one reset request");
            assertEquals(1, crossBorder.expenses().size());
            assertEquals(1, crossBorder.selectedExpense().id());
            assertEquals("", page.getByTestId("student-bill-search").inputValue());
            assertEquals("due-asc", page.getByTestId("student-bill-sort").inputValue());
            assertThat(page.getByTestId("tab-student")).hasAttribute("aria-selected", "true");
            page.waitForFunction("value => Math.abs(window.scrollY - value) <= 2", previousY);

            String selectedBillBeforeFailure = page.getByTestId("tuition-bill").innerText();
            String expiryBeforeFailure = page.getByTestId("channel-BANK_A").getAttribute("data-quote-expiry");
            page.route("**/student/quotes/refresh", route -> route.abort());
            page.getByTestId("refresh-quotes").click();
            assertThat(page.locator(".notice.request-error")).hasAttribute("role", "alert");
            assertThat(page.locator(".notice.request-error")).containsText("Unable to confirm the result");
            assertThat(page.getByTestId("refresh-quotes")).isEnabled();
            assertEquals(selectedBillBeforeFailure, page.getByTestId("tuition-bill").innerText());
            assertEquals(expiryBeforeFailure, page.getByTestId("channel-BANK_A").getAttribute("data-quote-expiry"));
            page.getByTestId("channel-BANK_A").evaluate("card => { card.dataset.quoteExpiry = String(Date.now() + 1200); }");
            page.waitForFunction("""
                    () => document.querySelector('[data-testid="channel-BANK_A"] [data-quote-live-status]')
                            .textContent.includes('Quote expired')
                    """);
            assertEquals(1, refreshRequests.size(), "Failed writes must not be retried automatically");
            assertTrue(navigations.isEmpty(), "Reset and failed refresh must not reload the main document: " + navigations);
            assertTrue(errors.isEmpty(), String.join(" | ", errors));
        }
    }

    @Test void inPlaceFormsKeepApprovalAndSandboxExecutionControlledAndEmergencyStopBlocksNewActions() {
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1000))) {
            Page page = context.newPage();
            var errors = new ArrayList<String>();
            var navigationRequests = new ArrayList<String>();
            var approvals = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(BASE_URL);
            page.getByTestId("tab-student").click();
            page.evaluate("() => { window.__inPlaceDocumentMarker = 'approval-sandbox-test'; }");
            page.onRequest(request -> {
                if (request.isNavigationRequest() && request.frame().parentFrame() == null) {
                    navigationRequests.add(request.url());
                }
                if ("POST".equals(request.method()) && request.url().endsWith("/approve")) {
                    approvals.add(request.url());
                }
            });

            page.getByTestId("plan-BANK_A").click();
            assertThat(page.getByTestId("tab-agent")).hasAttribute("aria-selected", "true");
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "AWAITING_APPROVAL");
            assertThat(page.getByTestId("latest-action")).containsText("BANK_A");
            assertThat(page.getByTestId("latest-receipt")).hasCount(0);

            page.onceDialog(confirm -> confirm.dismiss());
            page.getByTestId("approve-action").click();
            assertEquals(0, approvals.size(), "Dismissing approval must not submit a payment request");
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "AWAITING_APPROVAL");
            assertThat(page.getByTestId("latest-receipt")).hasCount(0);

            page.onceDialog(confirm -> confirm.accept());
            page.getByTestId("approve-action").click();
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "COMPLETED");
            assertThat(page.getByTestId("latest-receipt")).isVisible();
            assertThat(page.getByTestId("receipt-vnd-debit")).containsText("70,760,800.00 VND");
            assertThat(page.getByTestId("receipt-credit")).containsText("20,000.00 CNY");
            assertEquals(1, approvals.size(), "An accepted approval must execute through exactly one POST");
            String transactionId = page.getByTestId("latest-receipt").getAttribute("data-transaction-id");
            assertTrue(transactionId != null && !transactionId.isBlank());

            page.getByTestId("emergency-stop").click();
            assertThat(page.getByTestId("agent-state")).containsText("PAUSED");
            assertThat(page.getByTestId("audit-log")).containsText("EMERGENCY STOP");
            page.getByTestId("create-low-risk").click();
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "BLOCKED");
            assertThat(page.getByTestId("audit-log")).containsText("AGENT PAUSED");
            assertEquals(transactionId, page.getByTestId("latest-receipt").getAttribute("data-transaction-id"),
                    "Emergency Stop must not permit another sandbox receipt");
            assertEquals("approval-sandbox-test", page.evaluate("() => window.__inPlaceDocumentMarker"));
            assertTrue(navigationRequests.isEmpty(), "Controlled actions must update in place: " + navigationRequests);
            assertTrue(errors.isEmpty(), String.join(" | ", errors));
        }
    }

    private int addVerifiedBill(String title, String amount) {
        return crossBorder.addExpense("INSURANCE", title, CrossBorderService.SCHOOL_NAME,
                new BigDecimal(amount), "China", "CNY", CrossBorderService.SCHOOL_RECIPIENT_NAME,
                CrossBorderService.SCHOOL_RECIPIENT_BANK, CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE,
                CrossBorderService.SCHOOL_RECIPIENT, "REF-" + title.replace(' ', '-'),
                LocalDate.now().plusDays(30), null, null, null);
    }

    @Test void expenseAndCompactChannelFlowWorksInBrowser() {
        try(BrowserContext context=browser.newContext(new Browser.NewContextOptions().setViewportSize(1440,1000))) {
            Page page=context.newPage();
            var pageErrors=new ArrayList<String>();
            page.onPageError(error -> pageErrors.add(error));
            page.navigate(BASE_URL);
            page.getByTestId("tab-student").click();

            assertEquals(5,page.locator(".channel-list-row").count());
            assertEquals(0,page.getByTestId("payment-account-sort").count());
            assertThat(page.locator(".preference-form")).containsText("Priority and order");
            var firstChannel=page.locator(".channel-list-row").nth(0).boundingBox();
            var secondChannel=page.locator(".channel-list-row").nth(1).boundingBox();
            assertTrue(secondChannel.x > firstChannel.x);
            assertTrue(Math.abs(secondChannel.y - firstChannel.y) < 5);
            page.locator(".channel-list-row").first().getByRole(AriaRole.BUTTON,
                    new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Details")).click();
            assertThat(page.locator("dialog[open]")).containsText("SIMULATED QUOTE DETAIL");
            assertThat(page.locator("dialog[open]")).containsText("VND/CNY");
            page.locator("dialog[open] [data-close-dialog]").click();
            assertEquals(0,page.locator("dialog[open]").count());

            page.locator("[data-open-dialog='add-student-expense']").click();
            var dialog=page.locator("#add-student-expense");
            dialog.getByLabel("Education expense type").selectOption("DORMITORY");
            dialog.getByLabel("Bill name").fill("Dormitory deposit");
            dialog.getByLabel("Destination country").selectOption("United States");
            assertEquals("USD", dialog.getByLabel("Currency").inputValue());
            assertEquals(CrossBorderService.US_SCHOOL_NAME,
                    dialog.getByLabel("School or education provider").inputValue());
            assertEquals(CrossBorderService.US_SCHOOL_RECIPIENT_NAME,
                    dialog.getByLabel("Beneficiary legal name").inputValue());
            assertEquals(CrossBorderService.US_SCHOOL_RECIPIENT_BANK,
                    dialog.getByLabel("Receiving bank or payment provider").inputValue());
            assertEquals(CrossBorderService.US_SCHOOL_RECIPIENT_BANK_CODE,
                    dialog.getByLabel("SWIFT/BIC or bank routing code").inputValue());
            dialog.getByLabel("Amount").fill("2500.00");
            dialog.getByLabel("Payment reference").fill("DORM-2026-MINH");
            dialog.getByLabel("Due date").fill(LocalDate.now().plusDays(20).toString());
            dialog.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Locator.GetByRoleOptions()
                    .setName("Add and verify bill")).click();

            assertThat(page.locator(".student-expense-card.selected")).containsText("Dormitory deposit");
            assertThat(page.locator(".student-expense-card.selected")).containsText("VERIFIED BENEFICIARY");
            page.getByTestId("student-bill-search").fill("Pacific Demo College");
            assertEquals(1,page.locator("[data-bill-row]:visible").count());
            page.getByTestId("student-bill-clear").click();
            assertEquals(2,page.locator("[data-bill-row]:visible").count());
            var bills = page.getByTestId("student-expense-list");
            var firstBill = page.locator("[data-bill-row]:visible").nth(0).boundingBox();
            var secondBill = page.locator("[data-bill-row]:visible").nth(1).boundingBox();
            assertTrue(secondBill.x > firstBill.x, "Bills should be arranged horizontally on desktop");
            assertTrue(Math.abs(secondBill.y - firstBill.y) < 2, "Bills should share one row");
            bills.screenshot(new com.microsoft.playwright.Locator.ScreenshotOptions()
                    .setPath(Path.of("target", "student-bills-desktop.png")));

            page.setViewportSize(390, 844);
            firstBill = page.locator("[data-bill-row]:visible").nth(0).boundingBox();
            secondBill = page.locator("[data-bill-row]:visible").nth(1).boundingBox();
            assertTrue(secondBill.x > firstBill.x, "Bills should remain horizontal on mobile");
            assertTrue(Math.abs(secondBill.y - firstBill.y) < 2);
            assertTrue((Boolean) bills.evaluate("element => element.scrollWidth > element.clientWidth"),
                    "The bill container should allow horizontal scrolling");
            assertTrue((Boolean) page.evaluate("""
                    () => document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1
                    """), "The page should not overflow horizontally");
            bills.evaluate("element => { element.scrollLeft = element.scrollWidth; }");
            page.waitForFunction("""
                    () => document.querySelector('[data-testid="student-expense-list"]').scrollLeft > 0
                    """);
            page.getByTestId("language-vi").click();
            assertThat(page.locator("#student-finance")).containsText("Cuộn ngang để xem hóa đơn và nhà cung cấp");
            bills.screenshot(new com.microsoft.playwright.Locator.ScreenshotOptions()
                    .setPath(Path.of("target", "student-bills-mobile-vi.png")));
            page.getByTestId("language-en").click();
            bills.evaluate("element => { element.scrollLeft = 0; }");
            page.setViewportSize(1440, 1000);
            assertThat(page.getByTestId("tuition-bill")).containsText("2,500");
            assertThat(page.getByTestId("tuition-bill")).containsText("USD");
            assertThat(page.getByTestId("student-corridor")).containsText("United States");
            assertThat(page.getByTestId("student-currencies")).containsText("VND → USD");
            assertThat(page.getByTestId("channel-BANK_A")).containsText("65,640,500 VND");
            assertThat(page.getByTestId("channel-ALIPAY")).containsText("Unavailable for corridor");
            page.getByTestId("plan-BANK_A").click();
            assertThat(page.getByTestId("latest-action")).containsText("2,500.00 USD");
            assertThat(page.getByTestId("latest-action")).containsText(CrossBorderService.US_SCHOOL_RECIPIENT);

            page.getByTestId("tab-student").click();
            page.getByTestId("expense-2").getByRole(AriaRole.BUTTON,
                    new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Edit")).click();
            var editDialog=page.locator("#edit-student-expense-2");
            editDialog.getByLabel("Bill name").fill("Updated dormitory deposit");
            editDialog.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Locator.GetByRoleOptions()
                    .setName("Save and invalidate old plans")).click();
            assertThat(page.getByTestId("expense-2")).containsText("Updated dormitory deposit");
            page.getByTestId("tab-agent").click();
            assertEquals(0,page.getByTestId("latest-action").count());

            page.getByTestId("tab-student").click();
            page.onceDialog(confirm -> confirm.accept());
            page.getByTestId("expense-2").getByRole(AriaRole.BUTTON,
                    new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Archive")).click();
            assertThat(page.getByTestId("expense-2")).containsText("ARCHIVED");
            assertThat(page.locator(".student-expense-card.selected")).containsText("Tuition fee");
            page.getByTestId("student-bill-status-filter").selectOption("ARCHIVED");
            assertEquals(1,page.locator("[data-bill-row]:visible").count());
            assertThat(page.locator("[data-bill-row]:visible")).containsText("Updated dormitory deposit");
            page.getByTestId("student-bill-clear").click();
            page.getByTestId("expense-2").getByRole(AriaRole.BUTTON,
                    new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Restore")).click();
            page.waitForLoadState(LoadState.LOAD);
            assertThat(page.getByTestId("expense-2")).containsText("ACTIVE");

            page.getByTestId("language-vi").click();
            String languageState = (String) page.evaluate("""
                    () => JSON.stringify({
                      stored: localStorage.getItem('finbridge-language'),
                      lang: document.documentElement.lang,
                      pressed: document.querySelector('[data-testid=language-vi]').getAttribute('aria-pressed')
                    })
                    """);
            assertEquals("vi",page.locator("html").getAttribute("lang"),
                    String.join(" | ",pageErrors) + " · " + languageState);
            assertThat(page.locator("#student-finance")).containsText("Lập kế hoạch chi phí du học");
            assertThat(page.locator("#student-finance")).containsText("So sánh thông tin cần thiết");
        }
    }
}
