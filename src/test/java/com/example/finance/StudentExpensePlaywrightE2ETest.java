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
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
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
