package com.example.finance;

import com.microsoft.playwright.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
    "server.port=8170","spring.datasource.url=jdbc:h2:mem:bilingual_layout;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "app.demo-tools-enabled=true","app.shared-demo=true"})
class BilingualLayoutAcceptancePlaywrightTest {
    @Autowired DemoDataService demo; @Autowired PhaseFourService payments;
    @Autowired CrossBorderService border;
    @Autowired org.springframework.jdbc.core.JdbcTemplate db;
    @MockitoBean LlmIntentClient model;
    Playwright pw; Browser browser; Page page; List<String> errors;
    @BeforeEach void open(){demo.resetAll();when(model.enabled()).thenReturn(false);errors=new ArrayList<>();
        pw=Playwright.create();browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
        page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1440,1000));page.onPageError(errors::add);
        page.setDefaultTimeout(10000);
    }
    @AfterEach void close(){browser.close();pw.close();assertTrue(errors.isEmpty(),errors.toString());verify(model,never()).classify(anyString());}
    void capture(String name)throws Exception{Path dir=Path.of("docs/notification-layout/evidence");Files.createDirectories(dir);
        page.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(dir.resolve(name+".png")));}
    void noPageOverflow(){assertTrue((Boolean)page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));}
    void tab(String name){page.locator("[data-tab='"+name+"']:visible").first().click();}
    @Test void allTabsAndChatFitBothLanguagesAcrossDesktopTabletAndMobile()throws Exception{
        var plan=payments.createTuitionPlan("BANK_A");var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        page.navigate("http://localhost:8170/?action="+plan.id());
        for(int width:new int[]{360,390,768,1440})for(String lang:new String[]{"vi","en"}){
            page.setViewportSize(width,1000);page.getByTestId("language-"+lang).click();
            for(String area:new String[]{"dashboard","transactions","student","agent"}){
                tab(area);noPageOverflow();
                if(area.equals("student"))assertTrue((Boolean)page.locator(".planning-only").evaluateAll("es=>es.every(e=>e.scrollWidth<=e.clientWidth+1&&e.getBoundingClientRect().right<=e.closest('.channel-list-row').getBoundingClientRect().right)"),"plan button text fits "+width+" "+lang);
                assertThat(page.getByTestId("notification-bell")).isVisible();
                if(width==390||width==1440)capture(area+"-"+width+"-"+lang);
            }
            tab("dashboard");
            for(String view:new String[]{"accounts","planning","summary"}){
                page.locator("[data-dashboard-view='"+view+"']").click();noPageOverflow();
                if(!view.equals("summary")&&(width==390||width==1440))capture(view+"-"+width+"-"+lang);
            }
            page.getByTestId("assistant-launcher").click();
            assertThat(page.getByTestId("assistant-send-message")).isVisible();
            assertThat(page.getByTestId("assistant-panel")).isVisible();noPageOverflow();
            assertEquals(0,page.locator("[data-assistant-navigation], [data-assistant-language], [data-assistant-emergency]").count());
            if(width==390||width==1440){Path dir=Path.of("docs/notification-layout/evidence");page.getByTestId("assistant-panel").screenshot(new Locator.ScreenshotOptions().setPath(dir.resolve("chat-"+width+"-"+lang+".png")));}
            page.getByTestId("assistant-close").click();
        }
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertEquals(0,payments.sandboxTransactionCount());
    }
    @Test void freshAndExpiredComparisonCardsKeepAlignedActionsAndReadableDialogs()throws Exception{
        page.navigate("http://localhost:8170");
        for(String lang:new String[]{"vi","en"}){
            page.setViewportSize(1440,1000);page.getByTestId("language-"+lang).click();tab("student");
            assertThat(page.locator(".student-bill-pagination")).isHidden();
            assertTrue((Boolean)page.locator(".student-bill-list").evaluate("e=>!e.classList.contains('single-visible-bill')&&e.querySelector('[data-bill-row]').getBoundingClientRect().width<=320"));
            assertThat(page.locator(".payment-demo-entry")).hasCount(0);
            for(boolean expired:new boolean[]{false,true}){
                if(expired)page.evaluate("()=>{document.querySelectorAll('.channel-list-row').forEach(e=>e.dataset.quoteExpiry='1');renderQuoteExpiryStatuses();}");
                page.locator(".channel-list").scrollIntoViewIfNeeded();
                var positions=(List<?>)page.locator(".channel-list-row .channel-row-actions").evaluateAll("es=>es.map(e=>e.getBoundingClientRect().bottom)");
                double low=positions.stream().mapToDouble(n->((Number)n).doubleValue()).min().orElseThrow();
                double high=positions.stream().mapToDouble(n->((Number)n).doubleValue()).max().orElseThrow();
                assertEquals(low,high,2,"comparison action alignment: "+lang+" expired="+expired);
                Path evidence=Path.of("docs/notification-layout/evidence");Files.createDirectories(evidence);
                page.screenshot(new Page.ScreenshotOptions().setPath(evidence.resolve("channels-1440-"+lang+(expired?"-expired":"-fresh")+".png")));
                assertEquals("none",page.getByTestId("channel-BANK_A").locator(".channel-card-footer>small").evaluate("e=>getComputedStyle(e).textTransform"));
                assertTrue(((Number)page.getByTestId("channel-BANK_A").locator("[data-open-dialog]").evaluate("e=>parseFloat(getComputedStyle(e).fontSize)")).doubleValue()>=12);
                page.getByTestId("channel-BANK_A").locator("[data-open-dialog]").click();
                assertThat(page.locator("dialog[open]")).containsText("VND/CNY");
                for(int width:new int[]{360,390,1440}){
                    page.setViewportSize(width,844);noPageOverflow();
                    assertTrue((Boolean)page.locator("dialog[open]").evaluate("e=>{const r=e.getBoundingClientRect();return r.left>=0&&r.right<=innerWidth+1&&r.top>=0&&r.bottom<=innerHeight+1}"));
                }
                page.locator("dialog[open] [data-close-dialog]").click();page.setViewportSize(1440,1000);
            }
            page.reload();tab("student");
        }
        assertEquals(0,payments.sandboxTransactionCount());
    }
    @Test void unsupportedRouteExplainsTheBlockWithoutBreakingCardAlignment()throws Exception{
        border.addExpense("TUITION","USD tuition","Pacific Demo College",new java.math.BigDecimal("100"),
                "United States","USD","Pacific Demo College Bursar","Pacific Demo Bank","PDCMUS33XXX",
                "PDC-TUITION-USD","PDC-LAYOUT-USD",java.time.LocalDate.now().plusDays(20),null,null,null);
        page.navigate("http://localhost:8170");var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        for(String lang:new String[]{"vi","en"}){
            page.setViewportSize(1440,1000);page.getByTestId("language-"+lang).click();tab("student");
            var momo=page.getByTestId("channel-MOMO");assertThat(momo.locator("[data-select-channel]")).isDisabled();
            assertThat(momo).containsText(lang.equals("vi")?"Tài khoản đã liên kết":"Account connected");
            assertEquals(0,momo.getByTestId("plan-MOMO").count());
            var statusTops=(List<?>)page.locator(".channel-list-row .eligibility").evaluateAll("es=>es.map(e=>e.getBoundingClientRect().top)");
            assertEquals(statusTops.stream().mapToDouble(n->((Number)n).doubleValue()).min().orElseThrow(),
                    statusTops.stream().mapToDouble(n->((Number)n).doubleValue()).max().orElseThrow(),2,"status row alignment");
            var positions=(List<?>)page.locator(".channel-list-row .channel-row-actions").evaluateAll("es=>es.map(e=>e.getBoundingClientRect().bottom)");
            double low=positions.stream().mapToDouble(n->((Number)n).doubleValue()).min().orElseThrow();
            double high=positions.stream().mapToDouble(n->((Number)n).doubleValue()).max().orElseThrow();
            assertEquals(low,high,2);momo.scrollIntoViewIfNeeded();
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("docs/notification-layout/evidence/unsupported-1440-"+lang+".png")));
            for(int width:new int[]{360,390}){page.setViewportSize(width,844);noPageOverflow();}
        }
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertEquals(0,payments.sandboxTransactionCount());
    }
}
