package com.example.finance;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
        "server.port=8094",
        "spring.datasource.url=jdbc:h2:mem:finance_overview_e2e;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FinanceOverviewPlaywrightE2ETest {
    private static final String BASE_URL = "http://localhost:8094";
    @Autowired DemoDataService demoData;
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeAll
    void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel(System.getProperty("playwright.browser.channel", "chrome"))
                .setHeadless(true));
    }

    @AfterAll
    void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void openWorkspace() {
        demoData.resetAll();
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1000));
        page = context.newPage();
        page.setDefaultTimeout(10_000);
        page.navigate(BASE_URL);
    }

    @AfterEach
    void closeContext() {
        if (context != null) context.close();
    }

    @Test
    void manualCashAndWeeklyPlanUpdateTheProductionOverviewWithoutReload() {
        page.locator("[data-dashboard-view='accounts']").click();
        page.getByText("+ Add money source", new Page.GetByTextOptions().setExact(true)).click();
        page.locator("#add-money-source input[name='name']").fill("Cash wallet");
        page.locator("#add-money-source input[name='institution']").fill("Cash");
        page.locator("#add-money-source select[name='accountType']").selectOption("CASH");
        page.locator("#add-money-source input[name='balance']").fill("2000000");
        page.locator("#add-money-source button[type='submit']").click();

        assertThat(page.locator(".money-source-grid h3").filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText("Cash wallet"))).isVisible();
        assertThat(page.locator("[data-dashboard-view='accounts']")).hasAttribute("aria-pressed", "true");

        page.locator("[data-dashboard-view='planning']").click();
        page.getByText("+ Add plan", new Page.GetByTextOptions().setExact(true)).click();
        page.locator("#add-finance-plan input[name='title']").fill("Weekly groceries");
        page.locator("#add-finance-plan select[name='planType']").selectOption("RECURRING_BILL");
        page.locator("#add-finance-plan input[name='category']").fill("Groceries");
        page.locator("#add-finance-plan input[name='amount']").fill("1000000");
        page.locator("#add-finance-plan select[name='cadence']").selectOption("WEEKLY");
        page.locator("#add-finance-plan input[name='nextDueDate']").fill(LocalDate.now().toString());
        page.locator("#add-finance-plan select[name='fundingAccountId']").selectOption("CHECKING");
        page.locator("#add-finance-plan input[name='reserveFunds']").check();
        page.locator("#add-finance-plan button[type='submit']").click();

        assertThat(page.locator(".plan-list > .finance-plan-row h3").filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText("Weekly groceries"))).isVisible();
        assertThat(page.locator("[data-dashboard-view='planning']")).hasAttribute("aria-pressed", "true");
        page.getByText("+ Add budget", new Page.GetByTextOptions().setExact(true)).click();
        page.locator("#add-budget select[name='category']").selectOption("Groceries");
        page.locator("#add-budget input[name='monthlyLimit']").fill("1800000");
        page.locator("#add-budget button[type='submit']").click();
        assertThat(page.locator(".budget-editor")).containsText("Groceries");
        page.locator("[data-dashboard-view='summary']").click();
        assertThat(page.getByTestId("total-personal-balance")).containsText("317,000,000");
        assertThat(page.getByTestId("reserved-next-30")).containsText("13,300,000");
        assertThat(page.getByTestId("available-to-allocate")).containsText("300,700,000");

        page.getByTestId("language-vi").click();
        assertThat(page.getByText("Tài khoản & tiền mặt", new Page.GetByTextOptions().setExact(true))).isVisible();
    }
}
