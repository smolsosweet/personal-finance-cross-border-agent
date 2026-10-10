package com.example.finance;

import com.microsoft.playwright.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Actual local app/browser with synthetic H2; Gemini intent is mocked. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
    "server.port=8172","spring.datasource.url=jdbc:h2:mem:recipient_labels;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "app.demo-tools-enabled=true"})
class RecipientMatchLabelsPlaywrightTest {
    @Autowired DemoDataService demo; @Autowired PhaseFourService payments;
    @Autowired CrossBorderService border; @Autowired JdbcTemplate db;
    @MockitoBean LlmIntentClient model;
    Playwright pw; Browser browser; Page page; List<String> errors;
    @BeforeEach void open(){when(model.enabled()).thenReturn(true);demo.resetAll();errors=new ArrayList<>();
        pw=Playwright.create();browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
        page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1440,1000));page.onPageError(errors::add);page.setDefaultTimeout(15000);
    }
    @AfterEach void close(){browser.close();pw.close();assertTrue(errors.isEmpty(),errors.toString());}
    void idle(){page.waitForFunction("()=>!document.querySelector('.content').hasAttribute('aria-busy')");}
    void capture(String name)throws Exception{Path dir=TestEvidence.path("docs/recipient-labels/evidence");Files.createDirectories(dir);page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(name+".png")));
        if(name.startsWith("bill-") || name.startsWith("mismatch-")) page.getByTestId("recipient-verification").screenshot(new Locator.ScreenshotOptions().setPath(dir.resolve(name+"-status.png")));
        if(name.startsWith("bill-")) page.locator(".student-expense-card.selected").screenshot(new Locator.ScreenshotOptions().setPath(dir.resolve(name+"-card.png")));
        if(name.contains("audit-")) page.getByTestId("audit-log").locator("tr").filter(new Locator.FilterOptions().setHas(page.locator("[data-recipient-match=true][data-status=VERIFIED]"))).first().screenshot(new Locator.ScreenshotOptions().setPath(dir.resolve(name+"-match-row.png")));}
    void noOverflow(){assertTrue((Boolean)page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));}
    String matched(String lang){return lang.equals("vi")?"Khớp Danh bạ Nhà trường mô phỏng":"Matched against simulated School Registry";}
    @Test void billFiltersNotificationsAndMismatchUseHonestBilingualCopy()throws Exception{
        page.navigate("http://localhost:8172");var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        for(String lang:List.of("en","vi"))for(int width:new int[]{390,1440}){
            page.setViewportSize(width,1000);page.getByTestId("language-"+lang).click();page.locator("[data-tab=student]:visible").first().click();
            assertThat(page.locator(".verification-badge").first()).hasText(matched(lang));
            assertThat(page.getByTestId("recipient-verification")).containsText(matched(lang));
            assertThat(page.getByTestId("recipient-verification").locator("small")).hasText(lang.equals("vi")?"Danh bạ Nhà trường mô phỏng":"Simulated School Registry");
            assertThat(page.getByTestId("student-bill-verification-filter").locator("option[value='VERIFIED']")).hasText(matched(lang));
            assertFalse(page.getByTestId("recipient-verification").innerText().matches("(?is).*last checked.*"));
            noOverflow();capture("bill-"+width+"-"+lang);
            page.getByTestId("notification-bell").click();assertFalse(page.getByTestId("notification-center").innerText().contains("is verified"));
            assertFalse(page.getByTestId("notification-center").innerText().contains("đã được xác minh"));page.getByTestId("notification-center").locator("[data-close-dialog]").click();
        }
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
        db.update("UPDATE international_bills SET recipient_account='WRONG-ACCOUNT' WHERE selected=TRUE");
        page.reload();page.locator("[data-tab=student]:visible").first().click();
        for(String lang:List.of("en","vi")){
            page.getByTestId("language-"+lang).click();assertThat(page.getByTestId("recipient-verification")).containsText(lang.equals("vi")?"Người thụ hưởng không khớp":"Beneficiary mismatch");
            assertThat(page.getByTestId("recipient-verification")).containsText(lang.equals("vi")?"Danh bạ Nhà trường mô phỏng":"Simulated School Registry");
            assertEquals(0,page.getByTestId("plan-BANK_A").count());capture("mismatch-"+lang);
        }
        assertFalse(border.verifyRecipient().verified());assertEquals(0,payments.sandboxTransactionCount());
    }
    @Test void draftApprovalReceiptAndAuditKeepCodesButDisplayRegistryMatch()throws Exception{
        var plan=payments.createTuitionPlan("BANK_A");
        page.navigate("http://localhost:8172/?action="+plan.id()+"#agent-workspace");
        for(String lang:List.of("en","vi")){
            page.getByTestId("language-"+lang).click();page.getByTestId("audit-log").locator("summary").click();
            assertThat(page.getByTestId("audit-log")).containsText(matched(lang));capture("draft-audit-"+lang);
            page.getByTestId("audit-log").locator("summary").click();page.getByTestId("approve-action").click();
            assertThat(page.getByTestId("payment-confirm-dialog")).isVisible();capture("approval-"+lang);page.getByTestId("payment-confirm-cancel").click();
        }
        assertEquals("AWAITING_APPROVAL",payments.action(plan.id()).status());assertNull(payments.receiptForAction(plan.id()));
        PaymentApprovalControls.approve(page);idle();
        for(String lang:List.of("en","vi")){
            page.getByTestId("language-"+lang).click();assertThat(page.getByTestId("latest-receipt")).isVisible();
            page.getByTestId("audit-log").locator("summary").click();assertThat(page.getByTestId("audit-log")).containsText(matched(lang));assertThat(page.getByTestId("audit-log")).containsText(lang.equals("vi")?"Nhà cung cấp, người thụ hưởng, ngân hàng nhận, tài khoản":"Provider, beneficiary, receiving bank, account");capture("receipt-audit-"+lang);page.getByTestId("audit-log").locator("summary").click();
        }
        assertEquals("VERIFIED",db.queryForObject("SELECT status FROM audit_log WHERE event_type='RECIPIENT_VERIFICATION' AND reference_id=? ORDER BY occurred_at DESC FETCH FIRST 1 ROW ONLY",String.class,plan.id()));
        assertTrue(db.queryForObject("SELECT details FROM audit_log WHERE event_type='RECIPIENT_VERIFICATION' AND reference_id=? ORDER BY occurred_at DESC FETCH FIRST 1 ROW ONLY",String.class,plan.id()).contains("trusted demo registry"));
        assertEquals(1,payments.sandboxTransactionCount());
    }
    @Test void chatStatusAndComparisonUseNewCopyWithoutChangingStoredMessages()throws Exception{
        page.navigate("http://localhost:8172");var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        for(String lang:List.of("en","vi"))for(var intent:List.of(LlmIntent.Intent.CHECK_TUITION_STATUS,LlmIntent.Intent.COMPARE_TUITION_CHANNELS)){
            when(model.classify(anyString())).thenReturn(new LlmIntent(intent,LlmIntent.ChannelPreference.NONE,new BigDecimal("0.95"),LlmIntent.ClarificationCode.NONE));
            UiLanguageControls.select(page,lang);page.getByTestId("assistant-launcher").click();
            page.getByTestId("assistant-conversation-input").fill(intent==LlmIntent.Intent.CHECK_TUITION_STATUS?(lang.equals("vi")?"Học phí hiện thế nào?":"Check tuition status"):(lang.equals("vi")?"So sánh các kênh học phí":"Compare tuition channels"));
            page.getByTestId("assistant-send-message").click();idle();
            var reply=page.getByTestId("assistant-replies").locator(".message.assistant").last();
            if(intent==LlmIntent.Intent.COMPARE_TUITION_CHANNELS) assertThat(reply).containsText(matched(lang));
            else assertThat(reply).containsText("SZDU-2026-MINH");
            assertFalse(reply.textContent().contains("Verified tuition"));assertFalse(reply.textContent().contains("học phí đã xác minh"));
            capture("chat-"+intent+"-"+lang);page.getByTestId("assistant-close").click();
        }
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertEquals(0,payments.sandboxTransactionCount());
    }
}
