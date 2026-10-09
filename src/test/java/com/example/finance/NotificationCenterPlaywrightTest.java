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
 @Autowired PhaseFourService payments; @Autowired JdbcTemplate db;
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
 void screenshot(String name)throws Exception{Path dir=Path.of("docs/notifications");Files.createDirectories(dir);page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(name+".png")));}
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
   center().locator("[data-notification-key='transactions-review'] a").click();
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
}
