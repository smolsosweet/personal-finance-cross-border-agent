package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.microsoft.playwright.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={"server.port=8105","spring.datasource.url=jdbc:h2:mem:runway_browser;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.demo-tools-enabled=false"})
class RunwayPlaywrightTest {
    @Autowired DemoDataService demo;
    @Autowired JdbcTemplate db;
    @MockitoBean LlmIntentClient llm;
    Playwright pw;Browser browser;Page page;
    LlmIntent intent(){return new LlmIntent(LlmIntent.Intent.EXPLAIN_LIVING_EXPENSE_RUNWAY,LlmIntent.ChannelPreference.NONE,new BigDecimal("0.95"),LlmIntent.ClarificationCode.NONE);}
    @BeforeEach void setup(){reset(llm);when(llm.enabled()).thenReturn(true);when(llm.classify(anyString())).thenReturn(intent());demo.resetAll();pw=Playwright.create();browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));page=browser.newPage();page.onDialog(Dialog::accept);page.navigate("http://localhost:8105");page.getByTestId("language-vi").click();page.getByTestId("assistant-launcher").click();}
    @AfterEach void close(){browser.close();pw.close();}
    void idle(){page.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')");}
    void send(String text){page.getByTestId("assistant-conversation-input").fill(text);page.getByTestId("assistant-send-message").click();idle();}
    void confirm(String amount){page.getByTestId("runway-monthly-amount").fill(amount);page.getByTestId("runway-confirm").click();idle();}
    @Test void mobileStructuredInputConfirmationEditClearAndResetAreReadonly(){
        page.setViewportSize(390,844);var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        send("Sau khi đóng học phí, tiền còn lại đủ sinh hoạt mấy tháng?");
        assertThat(page.getByTestId("runway-currency")).hasValue("VND");assertThat(page.getByTestId("runway-currency")).hasAttribute("readonly","");
        assertThat(page.getByTestId("assistant-replies")).containsText("bao nhiêu VND mỗi tháng");
        confirm("8000000");assertThat(page.getByTestId("assistant-replies")).containsText("Khoảng 3.27 tháng");
        assertThat(page.getByTestId("runway-confirmed-monthly")).hasText("8000000.00 VND");
        verify(llm,times(1)).classify(anyString());send("Vậy đủ mấy tháng?");confirm("10000000");
        assertThat(page.getByTestId("assistant-replies")).containsText("Khoảng 2.62 tháng");
        assertThat(page.getByTestId("runway-confirmed-monthly")).hasText("10000000.00 VND");
        page.getByTestId("runway-clear").click();idle();assertThat(page.getByTestId("runway-confirmed-monthly")).hasCount(0);
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
        page.getByTestId("assistant-close").click();page.setViewportSize(1440,1000);page.getByTestId("tab-dashboard").click();page.getByTestId("reset-demo").click();idle();page.getByTestId("assistant-launcher").click();
        assertThat(page.getByTestId("runway-monthly-amount")).hasCount(0);assertThat(page.getByTestId("assistant-replies")).not().containsText("3.27 tháng");
    }
    @Test void editingWhileModelWaitsCancelsOldResponseAndKeepsNewAssumption() throws Exception{
        send("How many months after tuition?");confirm("8000000");var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1),finished=new CountDownLatch(1);
        when(llm.classify(anyString())).thenAnswer(call->{started.countDown();assertTrue(release.await(8,TimeUnit.SECONDS));finished.countDown();return intent();});
        page.getByTestId("assistant-conversation-input").fill("Vậy đủ mấy tháng?");page.getByTestId("assistant-send-message").click();assertTrue(started.await(5,TimeUnit.SECONDS));
        assertThat(page.getByTestId("runway-confirm")).isEnabled();confirm("10000000");release.countDown();assertTrue(finished.await(5,TimeUnit.SECONDS));page.waitForTimeout(250);
        assertThat(page.getByTestId("runway-confirmed-monthly")).hasText("10000000.00 VND");assertThat(page.getByTestId("assistant-replies")).containsText("2.62 tháng");assertThat(page.getByTestId("assistant-send-message")).isEnabled();
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
    }
    @Test void monthlyFormStaysWithinReachAfterLongHistoryOnDesktopAndMobile(){
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        send("Sau khi đóng học phí, tiền còn lại đủ sinh hoạt mấy tháng?");confirm("8000000");
        for(int i=0;i<8;i++)send("Vậy đủ mấy tháng?");
        for(int width:new int[]{1440,390}){
            page.setViewportSize(width,844);
            page.locator(".assistant-body").evaluate("body => body.scrollTop = body.scrollHeight");
            assertTrue((Boolean)page.getByTestId("runway-monthly-amount").evaluate("input => { const r=input.getBoundingClientRect(); const panel=input.closest('[data-assistant-panel]').getBoundingClientRect(); return r.top>=panel.top && r.bottom<=panel.bottom && !input.closest('.assistant-body'); }"));
            assertTrue((Boolean)page.getByTestId("runway-confirm").evaluate("button => { const r=button.getBoundingClientRect(); const dock=button.closest('[data-assistant-runway]').getBoundingClientRect(); return r.top>=dock.top && r.bottom<=dock.bottom; }"));
            assertThat(page.getByTestId("assistant-send-message")).isVisible();
            send("Nếu chi 9000000 VND mỗi tháng thì sao?");
            assertThat(page.getByTestId("runway-monthly-amount")).hasValue("");
            confirm("9000000");assertThat(page.getByTestId("runway-confirmed-monthly")).hasText("9000000.00 VND");
        }
        page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Path.of("target/runway-docked-mobile.png")));
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
    }
}
