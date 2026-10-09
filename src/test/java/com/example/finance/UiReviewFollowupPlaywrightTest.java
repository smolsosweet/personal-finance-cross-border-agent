package com.example.finance;

import com.microsoft.playwright.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
    "server.port=8137","spring.datasource.url=jdbc:h2:mem:ux_review_followup;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "app.demo-tools-enabled=true","app.shared-demo=true"})
class UiReviewFollowupPlaywrightTest {
    @Autowired DemoDataService demo;
    @Autowired CrossBorderService border;
    @Autowired org.springframework.jdbc.core.JdbcTemplate db;
    @MockitoBean LlmIntentClient llm;
    Playwright pw;Browser browser;Page page;
    List<String> errors=new ArrayList<>();
    @BeforeEach void setup() {
        demo.resetAll();reset(llm);when(llm.enabled()).thenReturn(true);
        pw=Playwright.create();browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
        page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844));
        page.setDefaultTimeout(10000);page.onPageError(errors::add);page.onDialog(Dialog::accept);
        page.navigate("http://localhost:8137");
    }
    @AfterEach void close() {
        if(browser!=null)browser.close();if(pw!=null)pw.close();
        assertTrue(errors.isEmpty(),errors.toString());verify(llm,never()).classify(anyString());
    }
    void tab(String value){page.locator("[data-tab='"+value+"']:visible").first().click();}
    void capture(String name) throws Exception {
        Path dir=Path.of("docs/ui-ux-followup/"+System.getProperty("ux.review.stage","after"));
        Files.createDirectories(dir);page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(name+".png")));
    }
    @Test void stickyTabsRemainClickableAfterScrollingInBothLanguages() {
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        for(int width:new int[]{360,390,768,1440}) {
            page.setViewportSize(width,844);
            for(String language:new String[]{"vi","en"}) {
                page.getByTestId("language-"+language).click();tab("student");
                page.evaluate("() => window.scrollTo({top:700,behavior:'instant'})");
                page.waitForFunction("() => !document.querySelector('.mobile-tabs').getClientRects().length || document.querySelector('.mobile-tabs').getBoundingClientRect().top >= document.querySelector('main>header').getBoundingClientRect().bottom-1");
                assertTrue((Boolean)page.evaluate("() => document.documentElement.scrollWidth<=innerWidth+1"));
                var button=page.locator("[data-tab='transactions']:visible").first();
                assertTrue((Boolean)button.evaluate("e => {const r=e.getBoundingClientRect();return e.contains(document.elementFromPoint(r.x+r.width/2,r.y+r.height/2));}"));
                button.click();assertThat(page.getByTestId("transaction-filters")).isVisible();
            }
        }
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
    }
    @Test void launcherNeverStealsPaginationTaps() throws Exception {
        for(int i=0;i<6;i++)GlobalAssistantFixtures.secondTuition(border);
        page.reload();
        for(int width:new int[]{360,390,768,1440}) {
            page.setViewportSize(width,844);
            for(String area:new String[]{"student","transactions"}) {
                tab(area);
                String prefix=area.equals("student")?"student-bill":"transaction";
                var next=page.getByTestId(prefix+"-next");
                assertThat(next).isEnabled();
                // Reproduce a natural scroll position with Next near the lower-right launcher.
                next.evaluate("e => {const r=e.getBoundingClientRect();window.scrollTo({top:scrollY+r.top-(innerHeight-42-r.height/2),behavior:'instant'});}");
                capture(prefix+"-"+width);
                for(double ratio:new double[]{0.2,0.5,0.8}) {
                    assertTrue((Boolean)next.evaluate("(e,ratio) => {const r=e.getBoundingClientRect();return e.contains(document.elementFromPoint(r.x+r.width*ratio,r.y+r.height/2));}",ratio),prefix+" hit target at "+width+" ratio "+ratio);
                }
                next.click();
                assertThat(page.getByTestId(prefix+"-page")).containsText("2");
                assertThat(page.getByTestId("assistant-panel")).isHidden();
                page.getByTestId(prefix+"-prev").click();
            }
        }
    }
    @Test void mobilePanelTrapsFocusAndDesktopRemainsNonmodal() throws Exception {
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        var panel=page.getByTestId("assistant-panel");
        page.getByTestId("assistant-launcher").click();
        capture("mobile-focus");
        assertThat(panel).hasAttribute("aria-modal","true");
        assertTrue((Boolean)page.evaluate("() => document.querySelector('.shell').inert"));
        page.getByTestId("assistant-conversation-input").fill("Bản nháp Budget của tôi");
        for(int i=0;i<32;i++) {
            page.keyboard().press(i<16?"Tab":"Shift+Tab");
            assertTrue((Boolean)panel.evaluate("p=>p.contains(document.activeElement)"),"Mobile focus stays in panel");
        }
        page.setViewportSize(390,430);
        assertThat(panel).hasAttribute("aria-modal","true");
        assertThat(page.getByTestId("assistant-send-message")).isVisible();
        page.setViewportSize(1440,900);
        assertThat(panel).hasAttribute("aria-modal","false");
        assertFalse((Boolean)page.evaluate("() => document.querySelector('.shell').inert"));
        page.getByTestId("tab-transactions").click();
        assertThat(panel).isVisible();assertThat(page.getByTestId("assistant-conversation-input")).hasValue("Bản nháp Budget của tôi");
        page.setViewportSize(360,844);
        assertThat(panel).hasAttribute("aria-modal","true");
        // Moving to a smaller screen returns background focus to the active dialog.
        assertTrue((Boolean)panel.evaluate("p=>p.contains(document.activeElement)"));
        page.keyboard().press("Escape");
        assertThat(panel).isHidden();assertFalse((Boolean)page.evaluate("() => document.querySelector('.shell').inert"));
        assertThat(page.getByTestId("assistant-launcher")).isFocused();
        page.getByTestId("assistant-launcher").click();
        UiLanguageControls.select(page,"vi");
        assertThat(page.getByTestId("assistant-emergency-stop")).isVisible();
        page.getByTestId("assistant-close").click();
        tab("student");
        assertThat(panel).isHidden();assertFalse((Boolean)page.evaluate("() => document.querySelector('.shell').inert"));
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
    }
    @Test void scopeSourcesAndNewCopyStayBilingualAndUserAuthoredTextStaysVerbatim() {
        db.update("UPDATE international_bills SET title='Budget Current balance nguyên văn' WHERE selected=TRUE");
        page.reload();
        for(String language:new String[]{"vi","en"}) {
            page.getByTestId("language-"+language).click();tab("dashboard");
            assertThat(page.getByTestId("current-account-balances")).containsText(language.equals("vi")?"Mỗi nguồn tiền chỉ được tính một lần":"Each source is counted once");
            assertThat(page.getByTestId("planning-source-heading")).hasText(language.equals("vi")?"Tiền dành cho kế hoạch":"Money available for planning");
            assertThat(page.getByTestId("environment-label")).hasText(language.equals("vi")?"Môi trường mô phỏng":"Simulation environment");
            tab("transactions");
            assertThat(page.getByTestId("transaction-history-scope")).containsText(language.equals("vi")?"Tất cả thời gian":"All time");
            page.getByTestId("transaction-type-filter").selectOption("Expense");
            assertThat(page.getByTestId("transaction-history-scope")).containsText(language.equals("vi")?"Bộ lọc":"Filters");
            page.getByTestId("transaction-clear-filters").click();
            tab("student");
            assertThat(page.getByTestId("tuition-bill")).containsText("Budget Current balance nguyên văn");
            page.getByTestId("assistant-launcher").click();
            assertThat(page.getByTestId("assistant-emergency-stop")).hasAttribute("title",language.equals("vi")?"Chặn thanh toán mới ngay. Không hoàn tác các khoản đã hoàn tất.":"Block new payments immediately. Completed payments are not reversed.");
            page.getByTestId("assistant-close").click();
        }
    }
}
