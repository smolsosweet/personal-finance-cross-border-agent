package com.example.finance;

import com.microsoft.playwright.*;
import java.math.BigDecimal;
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
 "server.port=8153","spring.datasource.url=jdbc:h2:mem:list_scroll_ui;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
class WorkspaceListScrollPlaywrightTest {
 @Autowired DemoDataService demo; @Autowired FinanceWorkspaceService workspace;
 @Autowired org.springframework.jdbc.core.JdbcTemplate db; @MockitoBean LlmIntentClient model;
 Playwright pw;Browser browser;Page page;List<String> errors=new ArrayList<>();
 @BeforeEach void open(){demo.resetAll();when(model.enabled()).thenReturn(false);
  pw=Playwright.create();browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
  page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1594,900));page.setDefaultTimeout(10000);page.onPageError(errors::add);
 }
 @AfterEach void close(){if(browser!=null)browser.close();if(pw!=null)pw.close();assertTrue(errors.isEmpty(),errors.toString());verify(model,never()).classify(anyString());}
 void navigate(){page.navigate("http://localhost:8153");}
 Locator accounts(){return page.locator(".money-source-grid");}
 void accountView(){page.locator("[data-dashboard-view=accounts]").click();}
 void fixture(){for(int i=0;i<15;i++)workspace.addManualAccount("Travel cash "+i,"Cash","CASH",null,BigDecimal.TEN);}
 boolean overflow(Locator region){return (Boolean)region.evaluate("e=>e.scrollHeight>e.clientHeight+2");}
 int height(Locator region){return ((Number)region.evaluate("e=>e.clientHeight")).intValue();}
 void screenshot(String name)throws Exception{Path dir=Path.of("target/scroll-layout");Files.createDirectories(dir);page.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(dir.resolve(name+".png")));}
 @Test void shortListsDoNotShowUnusableScrollControlsAndSeedCardsStayAligned()throws Exception{
  navigate();var budget=page.locator(".budget-snapshot-list");
  assertFalse(overflow(budget));assertTrue(height(budget)<200);
  assertThat(page.locator(".budget-snapshot-list + .list-scroll-down")).isHidden();
  accountView();var list=accounts();var button=page.locator(".money-source-grid + .list-scroll-down");
  if(overflow(list))assertThat(button).isEnabled();else assertThat(button).isHidden();
  List<?> sizes=(List<?>)list.locator(".money-source-card").evaluateAll("cards=>cards.map(e=>e.getBoundingClientRect().height)");
  double min=sizes.stream().mapToDouble(n->((Number)n).doubleValue()).min().orElseThrow();
  double max=sizes.stream().mapToDouble(n->((Number)n).doubleValue()).max().orElseThrow();assertEquals(min,max,1);
  assertTrue(height(list)<=480);screenshot("accounts-seed-desktop");
 }
 @Test void wheelAndScrollButtonActuallyMoveLongListsDesktopAndMobile()throws Exception{
  fixture();navigate();accountView();var list=accounts();var button=page.locator(".money-source-grid + .list-scroll-down");
  for(int width:new int[]{1594,768,390}){
   page.setViewportSize(width,900);list.evaluate("e=>e.scrollTop=0");assertTrue(overflow(list));
   list.scrollIntoViewIfNeeded();list.hover();page.mouse().wheel(0,220);
   page.waitForFunction("()=>document.querySelector('.money-source-grid').scrollTop>50");
   list.evaluate("e=>e.scrollTop=0");button.scrollIntoViewIfNeeded();double top=((Number)page.evaluate("()=>scrollY")).doubleValue();
   assertThat(button).isEnabled();button.click();page.waitForFunction("()=>document.querySelector('.money-source-grid').scrollTop>100");
   assertEquals(top,((Number)page.evaluate("()=>scrollY")).doubleValue(),1);
   list.focus();page.keyboard().press("End");assertThat(list).isFocused();
   assertTrue((Boolean)page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
  }
  screenshot("accounts-long-mobile");
 }
 @Test void controlsUpdateWhenContentChangesAndAfterSwitchingHiddenViews(){
  navigate();var budget=page.locator(".budget-snapshot-list");var button=page.locator(".budget-snapshot-list + .list-scroll-down");
  assertThat(button).isHidden();
  budget.evaluate("e=>{for(let i=0;i<20;i++){const item=document.createElement('div');item.textContent='Test budget row '+i;e.append(item);}}");
  assertThat(button).isVisible();assertThat(button).isEnabled();button.click();
  page.waitForFunction("()=>document.querySelector('.budget-snapshot-list').scrollTop>20");
  budget.evaluate("e=>e.replaceChildren(document.createElement('div'))");assertThat(button).isHidden();
  assertTrue(height(budget)<50);accountView();page.locator("[data-dashboard-view=summary]").click();assertThat(button).isHidden();
 }
 @Test void transactionHistoryAlsoSupportsWheelAndButtonOnMobile(){
  navigate();page.setViewportSize(390,844);page.locator("[data-tab=transactions]:visible").click();page.getByTestId("transaction-view-history").click();
  var list=page.locator("#transactions .table-scroll");var button=page.locator("#transactions .table-scroll + .list-scroll-down");
  assertTrue(overflow(list));list.scrollIntoViewIfNeeded();list.hover();page.mouse().wheel(0,220);
  page.waitForFunction("()=>document.querySelector('#transactions .table-scroll').scrollTop>30");
  list.evaluate("e=>e.scrollTop=0");assertThat(button).isEnabled();button.click();
  page.waitForFunction("()=>document.querySelector('#transactions .table-scroll').scrollTop>100");
 }
 @Test void overviewCardsArePairedWithoutOneCardSpanningTwoRows()throws Exception{
  navigate();var before=PersonalFinanceAiIntegrationTest.snapshot(db);
  for(String language:new String[]{"vi","en"}){
   page.getByTestId("language-"+language).click();
   var action=page.getByTestId("attention-center").boundingBox();var feed=page.getByTestId("proactive-feed").boundingBox();
   assertEquals(action.y,feed.y,1);assertEquals(action.height,feed.height,1);
   var cash=page.locator(".cashflow-snapshot").boundingBox();assertTrue(cash.y>=action.y+action.height);
   assertTrue(cash.width>action.width+feed.width);
  }
  page.setViewportSize(390,844);var action=page.getByTestId("attention-center").boundingBox();var feed=page.getByTestId("proactive-feed").boundingBox();
  assertTrue(feed.y>=action.y+action.height);assertEquals(action.width,feed.width,1);
  assertTrue((Boolean)page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
  assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));screenshot("overview-mobile");
 }
}
