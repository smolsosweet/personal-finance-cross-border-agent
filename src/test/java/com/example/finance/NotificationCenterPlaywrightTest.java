package com.example.finance;

import com.microsoft.playwright.*;
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
 "server.port=8164","spring.datasource.url=jdbc:h2:mem:notification_ui;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.demo-tools-enabled=true"})
class NotificationCenterPlaywrightTest {
 @Autowired DemoDataService demo; @Autowired TransactionService transactions;
 @Autowired CrossBorderService crossBorder; @Autowired PhaseFourService payments; @Autowired JdbcTemplate db;
 @MockitoBean LlmIntentClient model;
 Playwright pw; Browser browser; Page page; List<String> errors; List<String> posts;
 @BeforeEach void open(){demo.resetAll();when(model.enabled()).thenReturn(false);errors=new ArrayList<>();posts=new ArrayList<>();
  pw=Playwright.create();browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
  page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1366,768));page.setDefaultTimeout(10000);
  page.onPageError(errors::add);page.onRequest(r->{if(r.method().equals("POST"))posts.add(r.url());});page.navigate("http://localhost:8164");
 }
 @AfterEach void close(){browser.close();pw.close();assertTrue(errors.isEmpty(),errors.toString());verify(model,never()).classify(anyString());}
 Locator center(){return page.getByTestId("notification-center");}
 void bell(){page.getByTestId("notification-bell").click();try{assertThat(center()).isVisible();}catch(AssertionError ex){
  try{screenshot("open-failure");}catch(Exception ignored){};
  System.out.println("NOTIFICATION DEBUG "+page.evaluate("()=>({ready:document.readyState,open:Array.from(document.querySelectorAll('dialog[open]'),e=>e.id),bell:document.querySelector('[data-notifications-open]').outerHTML,active:document.activeElement.outerHTML})"));throw ex;}}
 void screenshot(String name)throws Exception{Path dir=Path.of("target/notification-timeline");Files.createDirectories(dir);page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(name+".png")));}
 @Test void responsiveBellReplacesOverviewClutterAndShowsUniqueCurrentItems()throws Exception{
  var before=PersonalFinanceAiIntegrationTest.snapshot(db);
  for(int width:new int[]{360,390,768,1366,1920})for(String lang:new String[]{"vi","en"}){
   page.setViewportSize(width,700);page.getByTestId("language-"+lang).click();
   assertThat(center()).isHidden();assertThat(page.getByTestId("attention-center")).isHidden();assertThat(page.getByTestId("proactive-feed")).isHidden();
   assertThat(page.getByTestId("notification-bell")).isInViewport();bell();
   assertThat(center().locator("h2")).hasText(lang.equals("vi")?"Thông báo":"Notifications");
   assertThat(center().locator("[data-notification-key='transactions-review']:visible")).hasCount(1);
   int count=center().locator("[data-notification-key]:visible").count();
   assertThat(page.getByTestId("notification-count")).hasText(String.valueOf(count));
   assertThat(center().locator(".workflow-link")).hasCount(0);
   assertTrue((Boolean)center().evaluate("e=>{let r=e.getBoundingClientRect();return r.left>=0&&r.right<=innerWidth&&r.top>=0&&r.bottom<=innerHeight}"));
   if(width==360||width==1366)screenshot("center-"+width+"-"+lang);
   page.keyboard().press("Escape");assertThat(page.getByTestId("notification-bell")).isFocused();
  }
  assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertTrue(posts.isEmpty());
 }
 @Test void opensFromEveryTabAndRoutesToTheCorrectReviewWithoutFinancialChanges(){
  var before=PersonalFinanceAiIntegrationTest.snapshot(db);
  for(String tab:List.of("dashboard","transactions","student","agent")){
   page.getByTestId("tab-"+tab).click();bell();
   center().locator("[data-notification-key='transactions-review']:visible a").click();
   assertThat(center()).isHidden();assertThat(page.getByTestId("notification-bell")).hasAttribute("aria-expanded","false");
   assertThat(page.getByTestId("transaction-view-review")).hasAttribute("aria-pressed","true");
  }
  bell();center().locator("a[href='#planning']").first().click();
  assertThat(page.locator("[data-dashboard-view=planning]")).hasAttribute("aria-pressed","true");
  assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertTrue(posts.isEmpty());
 }
 @Test void bankEventRefreshesNotificationContentsWithoutReloadOrExtraPost(){
  int before=((Number)transactions.dashboard().get("pendingReview")).intValue();
  page.getByTestId("tab-transactions").click();page.getByTestId("transaction-view-demo").click();page.getByTestId("simulate-medium").click();
  page.waitForFunction("()=>!document.querySelector('.content').hasAttribute('aria-busy')");
  assertEquals(before+1,((Number)transactions.dashboard().get("pendingReview")).intValue());
  bell();assertThat(center().locator("[data-notification-key='transactions-review']:visible")).containsText(String.valueOf(before+1));
  var snapshot=PersonalFinanceAiIntegrationTest.snapshot(db);
  page.keyboard().press("Escape");bell();assertEquals(snapshot,PersonalFinanceAiIntegrationTest.snapshot(db));
  assertEquals(1,posts.size());assertTrue(posts.get(0).endsWith("/events/simulate"));assertEquals(0,payments.sandboxTransactionCount());
 }
 @Test void escapeBackdropAndKeyboardKeepFocusAndDoNotHideInlinePaymentBlocks(){
  var draft=payments.createTuitionPlan("BANK_A");payments.emergencyStop();page.navigate("http://localhost:8164/?action="+draft.id()+"#agent-workspace");
  var before=PersonalFinanceAiIntegrationTest.snapshot(db);bell();
  for(int i=0;i<18;i++){page.keyboard().press("Tab");assertTrue((Boolean)center().evaluate("e=>e.contains(document.activeElement)"));}
  page.keyboard().press("Escape");assertThat(page.getByTestId("notification-bell")).isFocused();
  bell();page.mouse().click(2,2);assertThat(center()).isHidden();
  assertThat(page.getByTestId("workspace-paused")).isVisible();assertThat(page.getByTestId("payment-blocked")).isVisible();
  assertThat(page.getByTestId("approve-action")).hasCount(0);assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertTrue(posts.isEmpty());
 }

 Locator education(){return center().locator("[data-notification-key='education-bill-1']:visible");}
 void idle(){page.waitForFunction("()=>!document.querySelector('.content').hasAttribute('aria-busy')");}
 @Test void seenPersistsOnlyInThisBrowserAndNeverApprovesThePendingPlan(){
  var draft=payments.createTuitionPlan("BANK_A");page.navigate("http://localhost:8164/?action="+draft.id()+"#agent-workspace");
  var before=PersonalFinanceAiIntegrationTest.snapshot(db);bell();
  int unread=Integer.parseInt(page.getByTestId("notification-count").textContent());
  education().locator("[data-notification-mark-seen]").click();
  assertThat(education()).hasAttribute("data-notification-seen","true");
  assertThat(page.getByTestId("notification-count")).hasText(String.valueOf(unread-1));
  page.keyboard().press("Escape");assertThat(page.getByTestId("approve-action")).isVisible();
  assertEquals("AWAITING_APPROVAL",payments.action(draft.id()).status());assertEquals(0,payments.sandboxTransactionCount());
  page.reload();bell();assertThat(education()).hasAttribute("data-notification-seen","true");
  assertThat(education().locator("[data-notification-mark-seen]")).isDisabled();
  try(BrowserContext other=browser.newContext()){
   Page second=other.newPage();second.navigate("http://localhost:8164");second.getByTestId("notification-bell").click();
   assertThat(second.locator("[data-notification-key='education-bill-1']:visible")).hasAttribute("data-notification-seen","false");
  }
  assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertTrue(posts.isEmpty());
 }
 @Test void markAllKeepsTasksVisibleAndChangedTransactionUpdatesBecomeUnseenAgain()throws Exception{
  bell();var before=PersonalFinanceAiIntegrationTest.snapshot(db);
  page.getByTestId("notifications-mark-all").click();assertThat(page.getByTestId("notification-count")).isHidden();
  assertThat(education()).isVisible();assertThat(page.getByTestId("notifications-mark-all")).isDisabled();
  assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
  for(int i=0;i<18;i++){page.keyboard().press("Tab");assertTrue((Boolean)center().evaluate("e=>e.contains(document.activeElement)"));}
  page.getByTestId("language-vi").evaluate("e=>e.click()");
  assertThat(education().locator("[data-notification-read-status]")).hasText("Đã xem");
  page.keyboard().press("Escape");page.getByTestId("tab-transactions").click();page.getByTestId("transaction-view-demo").click();
  page.getByTestId("simulate-medium").click();idle();bell();
  assertThat(center().locator("[data-notification-key='transactions-review']:visible")).hasAttribute("data-notification-seen","false");
  assertThat(education()).hasAttribute("data-notification-seen","true");assertThat(page.getByTestId("notification-count")).isVisible();
  assertEquals(1,posts.size());assertEquals(0,payments.sandboxTransactionCount());
  Path dir=Path.of("target/notification-state");Files.createDirectories(dir);
  page.setViewportSize(390,844);page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve("changed-unseen-mobile-vi.png")));
 }
 @Test void actualVcbApprovalReplacesTheStaleReminderWithTheExactReceiptAndANewUnseenUpdate()throws Exception{
  var draft=payments.createTuitionPlan("VCB");page.navigate("http://localhost:8164/?action="+draft.id()+"#agent-workspace");
  bell();education().locator("[data-notification-mark-seen]").click();page.keyboard().press("Escape");
  assertEquals(0,payments.sandboxTransactionCount());PaymentApprovalControls.approve(page);idle();
  assertThat(page.getByTestId("latest-receipt")).isVisible();assertEquals("COMPLETED",payments.action(draft.id()).status());
  var receipt=payments.receiptForAction(draft.id());assertNotNull(receipt);assertEquals(1,payments.sandboxTransactionCount());
  assertEquals("Student payment completed",crossBorder.tuitionInsight().get("title"));
  bell();assertThat(education()).hasAttribute("data-notification-seen","false");
  assertThat(center().locator(".insight-list > [data-notification-key]:visible").first()).hasAttribute("data-notification-key","education-bill-1");
  assertThat(education()).hasAttribute("data-notification-kind","UPDATE");
  assertThat(education()).hasAttribute("data-notification-time",String.valueOf(receipt.createdAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()));
  assertThat(education()).containsText("Student payment completed");assertThat(education()).containsText(receipt.transactionId());
  assertThat(education()).not().containsText("Approval Mode is still required");
  assertThat(education().locator("a")).hasAttribute("href","?action="+draft.id()+"#agent-workspace");
  var completed=PersonalFinanceAiIntegrationTest.snapshot(db);
  Path dir=Path.of("target/notification-state");Files.createDirectories(dir);
  education().scrollIntoViewIfNeeded();page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve("completed-desktop-en.png")));
  page.getByTestId("language-vi").evaluate("e=>e.click()");
  assertThat(education()).containsText("Thanh toán du học đã hoàn tất");assertThat(education()).containsText("Không cần phê duyệt thêm");
  assertThat(education().locator("a")).hasText("Xem biên nhận →");
  page.setViewportSize(360,780);education().scrollIntoViewIfNeeded();page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve("completed-mobile-vi.png")));
  education().locator("[data-notification-mark-seen]").click();education().locator("a").click();
  assertThat(center()).isHidden();assertThat(page.getByTestId("latest-receipt")).containsText(receipt.transactionId());
  assertThat(page.getByTestId("approve-action")).hasCount(0);assertEquals(completed,PersonalFinanceAiIntegrationTest.snapshot(db));
  assertEquals(1,posts.stream().filter(url->url.endsWith("/approve")).count());
 }
 @Test void unavailableBrowserStorageStillAllowsSeenWithoutChangingFinancialState(){
  page.evaluate("""
    () => { const original=Storage.prototype.setItem;Storage.prototype.setItem=function(key,value){
      if(key==='finbridge-notifications-seen-v1') throw new Error('Storage disabled');
      return original.call(this,key,value);
    }; }
    """);
  var before=PersonalFinanceAiIntegrationTest.snapshot(db);bell();page.getByTestId("notifications-mark-all").click();
  assertThat(page.getByTestId("notification-count")).isHidden();assertThat(center().locator("[data-notification-storage-status]")).containsText("lasts only in this tab");page.keyboard().press("Escape");bell();
  assertThat(page.getByTestId("notification-count")).isHidden();assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertTrue(posts.isEmpty());
 }

 @Test void filtersSeparateActionStateFromSeenAndCountAllUnseenItems(){
  urgentBill();
  var before=PersonalFinanceAiIntegrationTest.snapshot(db);bell();
  String count=page.getByTestId("notification-count").textContent();
  center().locator("[data-notification-filter='UPDATE']").click();
  assertThat(center().locator("[data-notification-kind='ACTION']:visible")).hasCount(0);
  assertThat(center().locator("[data-notification-kind='UPDATE']:visible")).hasCount(1);
  assertThat(page.getByTestId("notification-count")).hasText(count);
  assertThat(center().locator("[data-notification-key='attention-Upcoming money is reserved']")).hasAttribute("data-notification-kind","UPDATE");
  center().locator("[data-notification-filter='ACTION']").click();
  assertThat(center().locator("[data-notification-kind='UPDATE']:visible")).hasCount(0);
  education().locator("[data-notification-mark-seen]").click();
  assertThat(education()).isVisible();assertThat(education().locator("[data-notification-state-label]")).hasText("Action soon");
  assertThat(education().locator("[data-notification-mark-seen]")).isHidden();
  page.getByTestId("notifications-mark-all").click();
  center().locator("[data-notification-filter='all']").click();
  assertThat(center().locator("[data-notification-seen='false']:visible")).hasCount(0);
  assertThat(page.getByTestId("notification-count")).isHidden();
  assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertTrue(posts.isEmpty());
 }

 @Test void informationalBodyAndKeyboardMarkSeenButActionAndDetailsDoNot(){
  java.math.BigDecimal spent=(java.math.BigDecimal)transactions.budgetSummary().stream().filter(i->"Utilities".equals(i.get("category"))).findFirst().orElseThrow().get("spent");
  db.update("UPDATE budgets SET monthly_limit=? WHERE category='Utilities'",spent.divide(new java.math.BigDecimal("0.9"),2,java.math.RoundingMode.HALF_UP));urgentBill();
  var before=PersonalFinanceAiIntegrationTest.snapshot(db);bell();
  Locator budget=center().locator("[data-notification-key='insight-Budget progress']");
  budget.locator("summary").click();assertThat(budget).hasAttribute("data-notification-seen","false");
  budget.locator("strong").click();assertThat(budget).hasAttribute("data-notification-seen","true");
  assertThat(budget.locator("[data-notification-mark-seen]")).isHidden();
  Locator safety=center().locator("[data-notification-key='attention-Upcoming money is reserved']");
  safety.focus();page.keyboard().press("Enter");assertThat(safety).hasAttribute("data-notification-seen","true");
  education().locator("strong").click();assertThat(education()).hasAttribute("data-notification-seen","false");
  assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertTrue(posts.isEmpty());
 }

 @Test void recordedTimeControlsOrderAndRemainsStableAcrossReloadSeenAndLanguage()throws Exception{
  db.update("UPDATE financial_accounts SET balance=2999999,balance_updated_at=? WHERE id='CHECKING'",java.time.LocalDateTime.now().plusMinutes(1));
  page.reload();bell();
  Locator items=center().locator(".insight-list > [data-notification-key]:visible");
  assertThat(items.first()).hasAttribute("data-notification-key","insight-Safety buffer needs attention");
  @SuppressWarnings("unchecked") List<String> order=(List<String>)items.evaluateAll("es=>es.map(e=>e.dataset.notificationKey+'|'+(e.dataset.notificationTime||''))");
  var before=PersonalFinanceAiIntegrationTest.snapshot(db);
  assertTrue((Boolean)items.evaluateAll("es=>es.every((e,i)=>!i||Number(es[i-1].dataset.notificationTime||0)>=Number(e.dataset.notificationTime||0))"));
  page.getByTestId("notifications-mark-all").click();
  page.getByTestId("language-vi").evaluate("e=>e.click()");
  assertThat(center().locator("[data-notification-filter='UPDATE']")).hasText("Cập nhật & gợi ý");
  assertThat(items.first().locator("[data-notification-state-label]")).hasText("Kiểm tra số dư");
  assertEquals(order,items.evaluateAll("es=>es.map(e=>e.dataset.notificationKey+'|'+(e.dataset.notificationTime||''))"));
  page.keyboard().press("Escape");page.reload();bell();
  assertEquals(order,items.evaluateAll("es=>es.map(e=>e.dataset.notificationKey+'|'+(e.dataset.notificationTime||''))"));
  assertThat(page.getByTestId("notification-count")).isHidden();
  page.setViewportSize(390,844);screenshot("seen-timeline-mobile-vi");
  assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));assertTrue(posts.isEmpty());
 }
 void urgentBill(){db.update("UPDATE international_bills SET due_date=? WHERE id=1",java.time.LocalDate.now().plusDays(5));crossBorder.refreshQuotes();page.reload();}
 @Test void deadlineUrgencyAndTwoDayMessageTranslateWithoutEnglishFragments(){
  bell();assertThat(education().locator(".priority")).hasText("INFO");page.keyboard().press("Escape");
  db.update("UPDATE international_bills SET due_date=? WHERE id=1",java.time.LocalDate.now().plusDays(6));crossBorder.refreshQuotes();page.reload();bell();
  assertThat(education()).containsText("is in 2 days");
  page.getByTestId("language-vi").evaluate("e=>e.click()");
  assertThat(education()).containsText("còn 2 ngày");assertThat(education()).not().containsText("is in 2 days");
  assertThat(education().locator(".priority")).hasText("Ưu tiên cao");
  assertThat(education().locator("[data-notification-state-label]")).hasText("Cần xử lý sớm");
 }
}
