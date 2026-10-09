package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Opt-in verification against port 8152 and an isolated synthetic database only. */
class UnifiedAccountsGeminiLiveIT {
    final Path evidence=Path.of("target/unified-gemini-evidence.json");
    final Path counter=Path.of("target/unified-gemini-requests.txt");
    final ObjectMapper mapper=new ObjectMapper();
    final List<Map<String,Object>> results=new ArrayList<>();
    JdbcTemplate db; Page page; int submissions;
    @Test void sharedBalancesDraftApprovalAndImmutableReceiptWithRealGemini() throws Exception {
        assertTrue(Files.exists(Path.of("target/unified-gemini-live.mv.db")));
        assertFalse(Files.exists(counter),"This bounded live run must not replay generations");
        db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:file:./target/unified-gemini-live;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE","sa",""));
        assertEquals(0,count("action_plans")); assertEquals(0,count("sandbox_transactions"));
        assertEquals(6,db.queryForObject("SELECT COUNT(*) FROM financial_accounts WHERE account_scope='PERSONAL' AND owner_profile_id=1",Integer.class));
        try(Playwright pw=Playwright.create(); Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true))) {
            page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1440,1000)); page.setDefaultTimeout(45000); page.onDialog(Dialog::accept);
            page.navigate("http://localhost:8152"); page.getByTestId("assistant-launcher").click();
            read("Hiện Bank A Everyday còn bao nhiêu tiền?","vi","EXPLAIN_CURRENT_BALANCE","100000000.00");
            send("Prepare the existing tuition payment draft using Bank A Everyday and Bank A International Transfer","en","CREATE_TUITION_PLAN","AWAITING_APPROVAL");
            assertEquals(1,count("action_plans")); assertEquals(0,count("sandbox_transactions"));
            String plan=db.queryForObject("SELECT id FROM action_plans",String.class);
            assertEquals("AWAITING_APPROVAL",db.queryForObject("SELECT status FROM action_plans WHERE id=?",String.class,plan));
            money("100000000");
            page.getByTestId("assistant-review-plan").click(); page.getByTestId("tab-agent").click();
            if(page.getByTestId("assistant-panel").isVisible())page.getByTestId("assistant-close").click();
            assertThat(page.getByTestId("latest-receipt")).hasCount(0);
            page.getByTestId("approve-action").click(); idle();
            assertEquals(1,count("sandbox_transactions")); money("29239200");
            String receipt=db.queryForObject("SELECT id FROM sandbox_transactions",String.class);
            assertThat(page.getByTestId("latest-receipt")).containsText("29,239,200.00");
            page.request().post("http://localhost:8152/agent/actions/"+plan+"/approve");
            assertEquals(1,count("sandbox_transactions")); money("29239200");
            page.getByTestId("tab-dashboard").click();
            assertThat(page.getByTestId("total-personal-balance")).containsText("244,239,200");
            assertThat(page.locator(".current-account-grid")).containsText("29,239,200");
            page.locator("[data-dashboard-view=accounts]").click();
            assertThat(page.getByTestId("money-source-CHECKING")).containsText("29,239,200");
            page.getByTestId("assistant-launcher").click();
            read("What is my current Bank A Everyday balance?","en","EXPLAIN_CURRENT_BALANCE","29239200.00");
            // A later synthetic salary separates the current balance from the historical receipt.
            page.request().post("http://localhost:8152/events/simulate",RequestOptions.create().setForm(FormData.create().set("scenario","income")));
            money("30139200");
            read("Sau khi đã thanh toán giao dịch "+receipt+" tôi còn bao nhiêu?","vi","EXPLAIN_RECEIPT_BALANCE","29239200.00");
            read("What is my current Bank A Everyday balance?","en","EXPLAIN_CURRENT_BALANCE","30139200.00");
            assertEquals(1,count("sandbox_transactions"));
            assertEquals(0,new java.math.BigDecimal("29239200").compareTo(db.queryForObject("SELECT vnd_balance_after FROM sandbox_transactions",java.math.BigDecimal.class)));
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/unified-gemini-final.png")));
        } finally {
            Files.writeString(evidence,mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("generationSubmissions",submissions,"cap",20,"results",results)));
            System.out.println("UNIFIED_GEMINI_LIVE submissions="+submissions+" evidence="+evidence);
        }
    }
    void money(String expected){assertEquals(0,new java.math.BigDecimal(expected).compareTo(db.queryForObject("SELECT balance FROM financial_accounts WHERE id='CHECKING'",java.math.BigDecimal.class)));}
    int count(String table){return db.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    void read(String text,String language,String intent,String expected)throws Exception{
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);send(text,language,intent,expected);assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
    }
    void send(String text,String language,String intent,String expected)throws Exception{
        assertTrue(submissions<20);submissions++;Files.writeString(counter,Integer.toString(submissions));
        int before=db.queryForObject("SELECT COUNT(*) FROM audit_log WHERE event_type='INTENT_CLASSIFIED'",Integer.class);
        UiLanguageControls.select(page,""+language);
        long start=System.nanoTime();page.getByTestId("assistant-conversation-input").fill(text);page.getByTestId("assistant-send-message").click();idle();
        long latency=(System.nanoTime()-start)/1_000_000;
        int after=db.queryForObject("SELECT COUNT(*) FROM audit_log WHERE event_type='INTENT_CLASSIFIED'",Integer.class);
        String actual=after==before+1?db.queryForObject("SELECT reason_code FROM audit_log WHERE event_type='INTENT_CLASSIFIED' ORDER BY occurred_at DESC,id DESC FETCH FIRST 1 ROW ONLY",String.class):"NO_CLASSIFIED_INTENT";
        String reply=page.getByTestId("assistant-replies").locator(".message.assistant").last().locator("[data-response-raw]").textContent();
        results.add(Map.of("question",text,"expectedIntent",intent,"actualIntent",actual,"schemaValid",after==before+1,"latencyMs",latency,"expectedEvidence",expected,"actualReply",reply,"plans",count("action_plans"),"payments",count("sandbox_transactions")));
        Files.writeString(evidence,mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("generationSubmissions",submissions,"cap",20,"results",results)));
        assertEquals(intent,actual,reply);assertTrue(reply.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT)),reply);
    }
    void idle(){page.waitForFunction("()=>!document.querySelector('.content').hasAttribute('aria-busy')");}
}
