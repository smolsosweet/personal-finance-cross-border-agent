package com.example.finance;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT, properties={
        "server.port=8098",
        "spring.datasource.url=jdbc:h2:mem:payment_source_e2e;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentSourcePlaywrightE2ETest {
    private static final String BASE_URL="http://localhost:8098";
    @Autowired DemoDataService demoData;
    private Playwright playwright;
    private Browser browser;

    @BeforeAll
    void setup() {
        demoData.resetAll();
        playwright=Playwright.create();
        browser=playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel(System.getProperty("playwright.browser.channel","chrome")).setHeadless(true));
    }

    @AfterAll
    void close() {
        if(browser!=null) browser.close();
        if(playwright!=null) playwright.close();
    }

    @Test
    void accountsCanBeFilteredAndSelectedWhileSuggestionsStayReferenceOnly() {
        try(BrowserContext context=browser.newContext(new Browser.NewContextOptions().setViewportSize(1440,1100))) {
            Page page=context.newPage();
            page.navigate(BASE_URL);
            page.getByTestId("tab-student").click();

            assertEquals(5,page.locator(".channel-list-row").count());
            assertThat(page.getByTestId("channel-BANK_B")).containsText("Not connected");
            assertEquals(0,page.getByTestId("channel-BANK_B").locator("button").count());

            page.getByTestId("payment-account-filter").selectOption("unavailable");
            assertEquals(0,page.locator(".channel-list-row:visible").count());
            assertThat(page.getByTestId("payment-account-empty")).isVisible();
            page.getByTestId("payment-account-filter").selectOption("insufficient");
            assertEquals(2,page.locator(".channel-list-row:visible").count());
            assertThat(page.getByTestId("channel-TCB")).containsText("Insufficient balance");
            assertThat(page.getByTestId("channel-MOMO")).containsText("Insufficient balance");
            assertThat(page.getByTestId("plan-TCB")).hasCount(0);
            assertThat(page.getByTestId("plan-MOMO")).hasCount(0);
            page.getByTestId("payment-account-filter").selectOption("payable");
            assertEquals(3,page.locator(".channel-list-row:visible").count());
            assertThat(page.getByTestId("channel-BANK_B")).isVisible();
            page.getByTestId("payment-account-filter").selectOption("all");

            assertThat(page.getByTestId("channel-VCB")).hasAttribute("data-balance", "82000000.00");
            assertThat(page.getByTestId("remaining-VCB")).containsText("11,153,028 VND");
            page.getByTestId("plan-VCB").click();
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status", "AWAITING_APPROVAL");
            assertThat(page.getByTestId("review-source")).containsText("Vietcombank Everyday");
            assertThat(page.getByTestId("review-channel")).containsText("Vietcombank");
            assertThat(page.getByTestId("review-total")).containsText("70,846,972");
            assertThat(page.getByTestId("latest-receipt")).hasCount(0);
        }
    }
}
