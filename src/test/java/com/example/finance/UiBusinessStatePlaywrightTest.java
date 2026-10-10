package com.example.finance;

import com.microsoft.playwright.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
    "server.port=8176","spring.datasource.url=jdbc:h2:mem:ui_state_browser;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","finbridge.llm.enabled=false"})
class UiBusinessStatePlaywrightTest {
    static final String BASE="http://localhost:8176";
    @Autowired DemoDataService demo;
    @Autowired CrossBorderService bills;
    @Autowired PhaseFourService payments;
    @Autowired JdbcTemplate db;
    @MockitoBean LlmIntentClient model;
    Playwright pw;Browser browser;Page page;List<String> errors;
    @BeforeEach void open(){demo.resetAll();when(model.enabled()).thenReturn(false);errors=new ArrayList<>();
        pw=Playwright.create();browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
        page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1366,900));page.onPageError(errors::add);
        page.setDefaultTimeout(12000);page.onDialog(Dialog::accept);page.navigate(BASE);}
    @AfterEach void close(){browser.close();pw.close();assertTrue(errors.isEmpty(),errors.toString());verify(model,never()).classify(anyString());}
    void idle(){page.waitForFunction("()=>!document.querySelector('.content').hasAttribute('aria-busy')");}
    void student(){page.locator("[data-tab=student]:visible").first().click();}
    void stage(int n){assertThat(page.getByTestId("tuition-progress")).hasAttribute("data-stage",String.valueOf(n));}
    Locator center(){return page.getByTestId("notification-center");}
    void bell(){page.getByTestId("notification-bell").click();assertThat(center()).isVisible();}
    void shot(String name)throws Exception{Path p=Path.of("target/ui-business-state");Files.createDirectories(p);page.screenshot(new Page.ScreenshotOptions().setPath(p.resolve(name+".png")));}
    void createBill(String name){
        page.locator("[data-open-dialog='add-student-expense']").click();
        Locator form=page.locator("#add-student-expense form");
        form.locator("[name=title]").fill(name);form.locator("[name=amount]").fill("1000");
        form.locator("[name=paymentReference]").fill(name);form.locator("[name=dueDate]").fill(LocalDate.now().plusDays(30).toString());
        form.locator("button[type=submit]").click();idle();stage(1);
    }
    void remove(int id,String mode){page.getByTestId("expense-"+id).locator("form[action$='/"+mode+"'] button").click();idle();}
    @Test void bilingualCreateCompareDraftApproveReceiptCancelAndArchiveWalkthrough()throws Exception {
        for(String language:List.of("en","vi")){
            demo.resetAll();page.navigate(BASE);page.setViewportSize(language.equals("vi")?390:1366,900);
            UiLanguageControls.select(page,language);student();
            createBill("New-"+language);assertThat(page.locator("#student-finance .channel-list")).hasCount(0);
            page.getByTestId("expense-2").locator(".student-bill-select").click();idle();stage(3);
            page.getByTestId("plan-BANK_A").click();idle();assertEquals(0,payments.sandboxTransactionCount());
            assertThat(page.getByTestId("latest-action")).hasAttribute("data-status","AWAITING_APPROVAL");
            PaymentApprovalControls.approve(page);idle();assertThat(page.getByTestId("latest-receipt")).isVisible();
            assertEquals(1,payments.sandboxTransactionCount());
            var receipt=payments.receiptForAction(page.getByTestId("latest-action").getAttribute("data-action-id"));
            bell();Locator item=center().locator("[data-notification-key='education-bill-2']:visible");
            assertThat(item).hasAttribute("data-notification-kind","UPDATE");assertThat(item).containsText(receipt.transactionId());
            assertThat(item.locator("[data-notification-state-label]")).hasText(language.equals("vi")?"Thanh toán hoàn tất":"Payment completed");
            var before=PersonalFinanceAiIntegrationTest.snapshot(db);item.locator("[data-notification-mark-seen]").click();
            assertThat(item).hasAttribute("data-notification-seen","true");assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
            shot("receipt-"+language);page.keyboard().press("Escape");student();stage(6);
            page.getByTestId("assistant-student-help").click();idle();assertThat(page.getByTestId("assistant-review-plan")).hasAttribute("data-action-id",receipt.actionId());
            page.getByTestId("assistant-close").click();createBill("Cancel-"+language);
            page.getByTestId("assistant-launcher").click();assertThat(page.getByTestId("assistant-review-plan")).hasCount(0);page.getByTestId("assistant-close").click();
            page.getByTestId("expense-3").locator(".student-bill-select").click();idle();remove(3,"cancel");
            assertEquals("CANCELLED",bills.expense(3).lifecycleStatus());assertFalse(bills.expense(3).selected());
            createBill("Archive-"+language);page.getByTestId("expense-4").locator(".student-bill-select").click();idle();remove(4,"archive");
            assertEquals("ARCHIVED",bills.expense(4).lifecycleStatus());assertFalse(bills.expense(4).selected());
            bell();assertThat(center().locator("[data-notification-key='education-bill-3']:visible")).hasCount(0);
            assertThat(center().locator("[data-notification-key='education-bill-4']:visible")).hasCount(0);shot("removed-"+language);page.keyboard().press("Escape");
            assertEquals(1,payments.sandboxTransactionCount());
        }
    }
    @Test void anotherSessionChangesBillAndOldReceiptShortcutDoesNotSurviveReload()throws Exception {
        var plan=payments.createTuitionPlan("BANK_A");payments.approveAndExecute(plan.id());
        page.reload();student();page.getByTestId("assistant-student-help").click();idle();
        assertThat(page.getByTestId("assistant-review-plan")).hasAttribute("data-action-id",plan.id());
        bills.addExpense("TUITION","Teammate bill",CrossBorderService.SCHOOL_NAME,new BigDecimal("1000"),"China","CNY",
            CrossBorderService.SCHOOL_RECIPIENT_NAME,CrossBorderService.SCHOOL_RECIPIENT_BANK,CrossBorderService.SCHOOL_RECIPIENT_BANK_CODE,
            CrossBorderService.SCHOOL_RECIPIENT,"TEAMMATE-STATE",LocalDate.now().plusDays(30),null,null,null);
        page.navigate(BASE);page.getByTestId("assistant-launcher").click();
        assertThat(page.getByTestId("assistant-review-plan")).hasCount(0);
        assertThat(page.getByTestId("assistant-replies")).containsText("Verified conversation scope updated");
        assertNotNull(payments.receiptForAction(plan.id()));shot("changed-bill-no-stale-shortcut");
    }
    @Test void seenKeepsUnresolvedActionsVisibleAndOrderingUsesRecordedTimes()throws Exception {
        payments.createTuitionPlan("BANK_A");payments.emergencyStop();page.reload();bell();
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        Locator items=center().locator(".insight-list > [data-notification-key]:visible");
        assertTrue((Boolean)items.evaluateAll("es=>es.every((e,i)=>!i||Number(es[i-1].dataset.notificationTime||0)>=Number(e.dataset.notificationTime||0))"));
        Object order=items.evaluateAll("es=>es.map(e=>e.dataset.notificationKey+'|'+(e.dataset.notificationTime||''))");
        page.getByTestId("notifications-mark-all").click();assertThat(page.getByTestId("notification-count")).isHidden();
        assertThat(center().locator("[data-notification-key='education-bill-1']:visible")).hasAttribute("data-notification-kind","ACTION");
        page.keyboard().press("Escape");page.reload();bell();assertEquals(order,items.evaluateAll("es=>es.map(e=>e.dataset.notificationKey+'|'+(e.dataset.notificationTime||''))"));
        assertThat(page.getByTestId("notification-count")).isHidden();assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));shot("seen-action-still-blocked");
    }
    @Test void openingNotificationsAfterATeammatePaysUsesFreshBusinessState()throws Exception {
        db.update("UPDATE international_bills SET due_date=? WHERE id=1",LocalDate.now().plusDays(6));bills.refreshQuotes();
        var plan=payments.createTuitionPlan("BANK_A");page.reload();
        var receipt=payments.approveAndExecute(plan.id());
        // No reload: the bell must not expose a cached approval reminder for the paid bill.
        bell();Locator item=center().locator("[data-notification-key='education-bill-1']:visible");
        assertThat(item).hasAttribute("data-notification-kind","UPDATE");assertThat(item).containsText(receipt.transactionId());
        assertThat(item.locator("[data-notification-state-label]")).hasText("Payment completed");shot("teammate-payment-fresh-notification");
    }
    @Test void openingNotificationsAfterATeammateCancelsOrArchivesDoesNotKeepTheirActionItems() {
        for(String mode:List.of("cancel","archive")){
            demo.resetAll();db.update("UPDATE international_bills SET due_date=? WHERE id=1",LocalDate.now().plusDays(6));bills.refreshQuotes();
            page.navigate(BASE);if(mode.equals("cancel"))bills.cancelExpense(1);else bills.archiveExpense(1);
            bell();assertThat(center().locator("[data-notification-key='education-bill-1']:visible")).hasCount(0);
            page.keyboard().press("Escape");
        }
    }
    @Test void refreshFailureRetainsTasksWithABilingualFreshnessWarningAndNoRetry() {
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);var requests=new ArrayList<String>();
        page.onRequest(r->{if(r.url().endsWith("/notifications"))requests.add(r.url());});
        page.route(BASE+"/notifications",Route::abort);bell();
        assertThat(center().locator("[data-notification-refresh-status]")).containsText("Showing the last loaded state");
        assertThat(center().locator("[data-notification-key='transactions-review']:visible")).isVisible();
        page.getByTestId("language-vi").evaluate("e=>e.click()");
        assertThat(center().locator("[data-notification-refresh-status]")).containsText("trạng thái lần tải trước");
        page.getByTestId("notifications-mark-all").click();assertThat(center().locator("[data-notification-key='transactions-review']:visible")).isVisible();
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertEquals(1,requests.size());
    }
}
