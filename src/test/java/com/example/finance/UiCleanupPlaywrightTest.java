package com.example.finance;

import com.microsoft.playwright.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Presentation regressions only: every fixture is synthetic and the model is disabled. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
 "server.port=8156","spring.datasource.url=jdbc:h2:mem:ux_cleanup;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
class UiCleanupPlaywrightTest {
 @Autowired DemoDataService demo; @Autowired PhaseFourService payments;
 @Autowired org.springframework.jdbc.core.JdbcTemplate db; @MockitoBean LlmIntentClient model;
 Playwright pw; Browser browser; Page page; List<String> errors;
 @BeforeEach void open(){demo.resetAll();when(model.enabled()).thenReturn(false);errors=new ArrayList<>();
  pw=Playwright.create();browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
  page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1366,768));page.setDefaultTimeout(10000);page.onPageError(errors::add);
 }
 @AfterEach void close(){browser.close();pw.close();assertTrue(errors.isEmpty(),errors.toString());verify(model,never()).classify(anyString());}
 void navigate(){page.navigate("http://localhost:8156");}
 void finished(){page.waitForFunction("()=>!document.querySelector('.content').hasAttribute('aria-busy')");}
 void screenshot(String name)throws Exception{Path dir=Path.of("docs/ui-ux-cleanup");Files.createDirectories(dir);page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(name+".png")));}
 @Test void modeAndSimulationRemainVisibleAcrossViewportAndLanguageChanges()throws Exception{
  payments.setMode("DELEGATED");var plan=payments.createTuitionPlan("BANK_A");
  page.navigate("http://localhost:8156/?action="+plan.id()+"#agent-workspace");
  var snapshot=PersonalFinanceAiIntegrationTest.snapshot(db);
  for(int width:new int[]{1366,1024,768,390,360}){
   page.setViewportSize(width,844);
   for(String language:new String[]{"en","vi"}){
    page.getByTestId("language-"+language).click();
    assertThat(page.getByTestId("global-permission-mode")).containsText(language.equals("en")?"Delegated":"Ủy quyền");
    assertThat(page.getByTestId("review-permission")).containsText(language.equals("en")?"Approval":"Phê duyệt");
    assertThat(page.getByTestId("agent-state")).isVisible();assertThat(page.getByTestId("environment-label")).isVisible();
    assertFalse((Boolean)page.getByTestId("payment-demo-tools").evaluate("e=>e.open"));
    assertTrue((Boolean)page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
    page.getByTestId("assistant-launcher").click();
    assertThat(page.getByTestId("assistant-environment-label")).isInViewport();
    assertThat(page.getByTestId("assistant-permission-mode")).containsText(language.equals("en")?"Delegated":"Ủy quyền");
    assertThat(page.getByTestId("assistant-conversation-input")).isInViewport();
    page.getByTestId("assistant-close").click();
   }
  }
  screenshot("mobile-review-vi");assertEquals(snapshot,PersonalFinanceAiIntegrationTest.snapshot(db));assertEquals(0,payments.sandboxTransactionCount());
 }
 @Test void blockedReasonRetainsBackendEvidenceAndPauseIsGlobal()throws Exception{
  var plan=payments.createTuitionPlan("BANK_A");payments.emergencyStop();
  page.navigate("http://localhost:8156/?action="+plan.id()+"#agent-workspace");
  var reason=page.locator("[data-payment-reason]").first();
  assertThat(reason).hasText("Emergency Stop is active, so new actions are blocked.");
  assertThat(page.getByTestId("payment-reason-code")).hasText("AGENT PAUSED");
  assertThat(page.getByTestId("approve-action")).hasCount(0);
  page.getByTestId("language-vi").click();assertThat(reason).hasText("Dừng khẩn cấp đang bật, nên các tác vụ mới bị chặn.");
  page.setViewportSize(390,844);
  for(String tab:new String[]{"dashboard","transactions","student","agent"}){
   page.locator(".mobile-tabs [data-tab="+tab+"]").click();
   assertThat(page.getByTestId("workspace-paused")).isVisible();assertThat(page.getByTestId("agent-state")).isVisible();
  }
  screenshot("mobile-paused-vi");
  page.getByTestId("emergency-resume").click();finished();assertThat(page.getByTestId("workspace-paused")).hasCount(0);
  assertThat(page.getByTestId("approve-action")).isEnabled();assertEquals(0,payments.sandboxTransactionCount());
 }
 @Test void unknownBackendExplanationIsNotReplacedByInventedAdvice(){
  var plan=payments.createTuitionPlan("BANK_A");payments.emergencyStop();
  page.navigate("http://localhost:8156/?action="+plan.id()+"#agent-workspace");
  var reason=page.locator("[data-payment-reason]").first();
  reason.evaluate("e=>{e.dataset.backendExplanation='Fixture backend explanation';e.dataset.reason='UNKNOWN_FIXTURE';e.textContent='Fixture backend explanation'}");
  page.getByTestId("language-vi").click();assertThat(reason).hasText("Fixture backend explanation");
  page.getByTestId("language-en").click();assertThat(reason).hasText("Fixture backend explanation");
 }
 @Test void guidedApprovalAndDelegatedResultKeepModeReceiptAndAuditConsistent()throws Exception{
  navigate();assertThat(page.getByTestId("global-permission-mode")).containsText("Approval");
  page.getByTestId("tab-student").click();page.getByTestId("plan-BANK_A").click();finished();
  assertThat(page.getByTestId("latest-action")).hasAttribute("data-status","AWAITING_APPROVAL");
  assertThat(page.getByTestId("payment-summary")).containsText("70,760,800");
  assertThat(page.getByTestId("payment-summary")).containsText("Bank A International Transfer");
  assertThat(page.getByTestId("payment-summary")).containsText(CrossBorderService.SCHOOL_RECIPIENT_NAME);
  assertEquals(0,payments.sandboxTransactionCount());
  page.onceDialog(Dialog::dismiss);page.getByTestId("approve-action").click();assertEquals(0,payments.sandboxTransactionCount());
  page.onceDialog(Dialog::accept);page.getByTestId("approve-action").click();finished();
  assertThat(page.getByTestId("latest-action")).hasAttribute("data-status","COMPLETED");
  assertThat(page.getByTestId("latest-receipt")).containsText("29,239,200.00");
  page.getByTestId("audit-log").locator("summary").click();
  assertThat(page.getByTestId("audit-log")).containsText("SANDBOX EXECUTED");screenshot("desktop-receipt-audit-en");
  page.getByTestId("payment-demo-tools").locator("summary").first().click();
  page.locator("button[name=mode][value=DELEGATED]").click();finished();
  assertThat(page.getByTestId("global-permission-mode")).containsText("Delegated");
  page.getByTestId("create-low-risk").click();finished();
  assertThat(page.getByTestId("review-permission")).containsText("Delegated");
  assertThat(page.getByTestId("latest-action")).hasAttribute("data-status","COMPLETED");
  assertThat(page.getByTestId("latest-receipt")).containsText("250,000.00");
  assertThat(page.getByTestId("approve-action")).hasCount(0);assertEquals(2,payments.sandboxTransactionCount());
  page.getByTestId("emergency-stop").click();finished();assertThat(page.getByTestId("workspace-paused")).isVisible();
  page.getByTestId("create-low-risk").click();finished();
  assertThat(page.getByTestId("latest-action")).hasAttribute("data-status","BLOCKED");
  assertThat(page.getByTestId("payment-reason-code")).hasText("AGENT PAUSED");assertEquals(2,payments.sandboxTransactionCount());
 }
 @Test void pendingGenericActionHasVisibleProgressAndNoDuplicateRequest(){
  navigate();page.getByTestId("tab-student").click();
  AtomicReference<Route> pending=new AtomicReference<>();List<String> requests=new ArrayList<>();
  page.route("**/student/quotes/refresh",route->{pending.set(route);requests.add(route.request().url());});
  page.getByTestId("refresh-quotes").click();page.waitForCondition(()->pending.get()!=null);
  assertThat(page.locator(".request-processing")).hasText("Processing…");
  assertThat(page.getByTestId("refresh-quotes")).isDisabled();assertEquals(1,requests.size());
  pending.get().resume();finished();assertThat(page.locator(".request-processing")).hasCount(0);
  assertThat(page.getByTestId("refresh-quotes")).isEnabled();assertEquals(0,payments.sandboxTransactionCount());
 }
}
