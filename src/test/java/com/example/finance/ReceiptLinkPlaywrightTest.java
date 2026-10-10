package com.example.finance;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.FilePayload;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Offline browser acceptance: fixed synthetic extraction stub, real HTTP capability and save path. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
    "server.port=8153","spring.datasource.url=jdbc:h2:mem:tc07_browser;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "finbridge.llm.enabled=false"})
class ReceiptLinkPlaywrightTest {
    @Autowired DemoDataService demo;
    @Autowired TransactionService transactions;
    @Autowired org.springframework.jdbc.core.JdbcTemplate db;
    @MockitoBean GeminiDocumentExtractionService extraction;
    @MockitoBean LlmIntentClient llm;
    Playwright pw; Browser browser; Page page;
    Path evidence;
    @BeforeEach void setup() throws Exception {
        demo.resetAll(); reset(extraction,llm); when(llm.enabled()).thenReturn(false);
        transactions.recordManualTransaction("TC07 Cafe",new BigDecimal("50000"),"VND",LocalDate.now(),"Cash","Food & Drinks","");
        when(extraction.extract(any(),eq("transaction"))).thenReturn(new DocumentExtractionService.Result(Map.of(
                "merchant","TC07 Cafe","amount","50000","currency","VND","occurredAt",LocalDate.now().toString(),
                "paymentMethod","Cash","category","Food & Drinks","reference",""),0.99,"gemini"));
        evidence=Files.createDirectories(Path.of("target","tc07-browser",UUID.randomUUID().toString()));
        pw=Playwright.create(); browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(!Boolean.getBoolean("tc07.headed")));
        page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844)); page.onDialog(Dialog::accept);
        page.navigate("http://localhost:8153");
    }
    @AfterEach void close() {
        if(browser!=null)browser.close(); if(pw!=null)pw.close();
        verify(llm,never()).classify(anyString());
    }
    void upload() {
        page.locator("[data-tab='transactions']:visible").first().click();
        page.locator("[data-open-dialog='add-transaction']:visible").first().click();
        page.locator("[data-entry-ai]").click();
        page.locator("#add-transaction [data-ai-file]").setInputFiles(new FilePayload("synthetic-receipt.png","image/png",
                Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX1cAAAAASUVORK5CYII=")));
        page.locator("#add-transaction [data-ai-extract]").click();
        assertThat(page.locator("[data-manual-transaction]")).isVisible();
        assertThat(page.locator("#add-transaction [data-ai-review-note]")).isVisible();
        assertThat(page.locator("[data-receipt-capability]")).hasValue(java.util.regex.Pattern.compile("[0-9a-f-]{36}"));
        page.locator("[data-manual-transaction] button[type='submit']").click();
        assertThat(page.getByTestId("receipt-result")).isVisible();
    }
    @Test void fixedSampleWorksWithGeminiDisabledWithoutAnExtractionStub() throws Exception {
        reset(extraction);
        int before=db.queryForObject("SELECT COUNT(*) FROM transactions",Integer.class);
        page.locator("[data-tab='transactions']:visible").first().click();
        page.locator("[data-open-dialog='add-transaction']:visible").first().click();
        page.locator("[data-entry-ai]").click();
        page.locator("[data-receipt-sample]").click();
        assertThat(page.locator("[data-manual-transaction]")).isVisible();
        assertThat(page.locator("#add-transaction [data-ai-status]")).containsText("no Gemini request");
        assertThat(page.locator("#add-transaction [data-ai-review-note]")).containsText("Sample receipt");
        page.locator("[data-manual-transaction] button[type='submit']").click();
        assertThat(page.getByTestId("receipt-result")).containsText("Receipt linked to the existing expense");
        assertEquals(before,db.queryForObject("SELECT COUNT(*) FROM transactions",Integer.class));
        verifyNoInteractions(extraction);
        page.screenshot(new Page.ScreenshotOptions().setPath(evidence.resolve("offline-sample-linked.png")));
    }
    @Test void tc07OfflineWalkthroughInVietnameseAndEnglish() throws Exception {
        page.getByTestId("language-vi").click();
        int before=db.queryForObject("SELECT COUNT(*) FROM transactions",Integer.class);
        upload();
        assertThat(page.getByTestId("receipt-result")).containsText("Biên lai đã liên kết");
        assertEquals(before,db.queryForObject("SELECT COUNT(*) FROM transactions",Integer.class));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM receipt_evidence",Integer.class));
        page.screenshot(new Page.ScreenshotOptions().setPath(evidence.resolve("vi-linked.png")));
        page.getByTestId("language-en").click();
        assertThat(page.getByTestId("receipt-result")).containsText("Receipt linked to the existing expense");
        assertThat(page.getByTestId("receipt-different-purchase")).hasText("This is a different purchase");
        page.getByTestId("receipt-different-purchase").click();
        assertThat(page.getByTestId("receipt-result")).containsText("A different purchase was recorded once");
        assertEquals(before+1,db.queryForObject("SELECT COUNT(*) FROM transactions",Integer.class));
        page.screenshot(new Page.ScreenshotOptions().setPath(evidence.resolve("en-different-purchase.png")));
        upload();
        assertThat(page.getByTestId("receipt-result")).containsText("Similar expenses found");
        assertEquals(2,page.locator("#receipt-result form[action='/transactions/receipt/link']").count());
        assertEquals(before+1,db.queryForObject("SELECT COUNT(*) FROM transactions",Integer.class));
        page.setViewportSize(1440,900);
        page.screenshot(new Page.ScreenshotOptions().setPath(evidence.resolve("en-third-upload-asks.png")));
        assertTrue((Boolean)page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        page.getByTestId("language-vi").click();
        assertThat(page.getByTestId("receipt-result")).containsText("Có khoản chi tương tự");
        assertThat(page.getByTestId("receipt-different-purchase")).hasText("Đây là khoản chi khác");
        page.screenshot(new Page.ScreenshotOptions().setPath(evidence.resolve("vi-third-upload-asks.png")));
    }
}