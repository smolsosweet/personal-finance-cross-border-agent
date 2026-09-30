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

            assertEquals(5,page.locator(".payment-account").count());
            assertThat(page.getByTestId("channel-BANK_B")).containsText("Reference only");
            assertEquals(0,page.getByTestId("channel-BANK_B").locator("button").count());

            page.getByTestId("payment-account-filter").selectOption("unavailable");
            assertEquals(2,page.locator(".payment-account:visible").count());
            page.getByTestId("payment-account-filter").selectOption("insufficient");
            assertEquals(1,page.locator(".payment-account:visible").count());
            assertThat(page.getByTestId("payment-account-TCB_VND")).containsText("Needs 28,760,800 VND more");
            page.getByTestId("payment-account-filter").selectOption("all");

            page.getByTestId("payment-account-MOMO_VND").getByRole(com.microsoft.playwright.options.AriaRole.BUTTON).click();
            assertEquals(2,page.locator(".payment-account:visible").count());
            page.getByTestId("payment-account-filter").selectOption("all");

            page.getByTestId("select-account-VCB_VND").click();
            assertThat(page.getByTestId("payment-balance")).containsText("82,000,000 VND");
            assertThat(page.getByTestId("remaining-BANK_A")).containsText("11,239,200 VND");

            page.getByTestId("plan-BANK_A").click();
            assertThat(page.getByTestId("latest-action")).containsText("VCB_VND");
        }
    }
}
