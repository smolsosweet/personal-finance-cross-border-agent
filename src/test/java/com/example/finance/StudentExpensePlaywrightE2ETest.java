package com.example.finance;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT, properties={
        "server.port=8096",
        "spring.datasource.url=jdbc:h2:mem:student_expense_e2e;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudentExpensePlaywrightE2ETest {
    private static final String BASE_URL="http://localhost:8096";
    @Autowired DemoDataService demoData;
    private Playwright playwright;
    private Browser browser;

    @BeforeAll void setup() {
        demoData.resetAll();
        playwright=Playwright.create();
        browser=playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel(System.getProperty("playwright.browser.channel","chrome")).setHeadless(true));
    }

    @AfterAll void close() {
        if(browser!=null) browser.close();
        if(playwright!=null) playwright.close();
    }

    @Test void expenseAndCompactChannelFlowWorksInBrowser() {
        try(BrowserContext context=browser.newContext(new Browser.NewContextOptions().setViewportSize(1440,1000))) {
            Page page=context.newPage();
            page.navigate(BASE_URL);
            page.getByTestId("tab-student").click();

            assertEquals(5,page.locator(".channel-list-row").count());
            page.locator(".channel-list-row").first().getByRole(AriaRole.BUTTON,
                    new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Details")).click();
            assertThat(page.locator("dialog[open]")).containsText("SIMULATED QUOTE DETAIL");
            assertThat(page.locator("dialog[open]")).containsText("VND/CNY");
            page.locator("dialog[open] [data-close-dialog]").click();
            assertEquals(0,page.locator("dialog[open]").count());

            page.locator("[data-open-dialog='add-student-expense']").click();
            var dialog=page.locator("#add-student-expense");
            dialog.getByLabel("Expense type").selectOption("DORMITORY");
            dialog.getByLabel("Expense name").fill("Dormitory deposit");
            dialog.getByLabel("Amount").fill("2500.00");
            dialog.getByLabel("Payment reference").fill("DORM-2026-MINH");
            dialog.getByLabel("Due date").fill(LocalDate.now().plusDays(20).toString());
            dialog.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Locator.GetByRoleOptions()
                    .setName("Add and compare expense")).click();

            assertThat(page.locator(".student-expense-card.selected")).containsText("Dormitory deposit");
            assertThat(page.getByTestId("tuition-bill")).containsText("2,500");
            assertThat(page.getByTestId("tuition-bill")).containsText("CNY");
            assertThat(page.getByTestId("channel-BANK_A")).containsText("9,037,600 VND");

            page.getByTestId("language-vi").click();
            assertThat(page.locator("#student-finance")).containsText("Lập kế hoạch chi phí du học");
            assertThat(page.locator("#student-finance")).containsText("So sánh thông tin cần thiết");
        }
    }
}
