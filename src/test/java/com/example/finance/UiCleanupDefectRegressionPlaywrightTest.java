package com.example.finance;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.BoundingBox;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** D1-D3 only: real browser, synthetic isolated database, no model calls or approvals. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
 "server.port=8159","spring.datasource.url=jdbc:h2:mem:ux_cleanup_defects;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
class UiCleanupDefectRegressionPlaywrightTest {
 @Autowired DemoDataService demo; @Autowired PhaseFourService payments;
 @MockitoBean LlmIntentClient model;
 Playwright pw; Browser browser; Page page; List<String> errors;
 @BeforeEach void open(){
  demo.resetAll();when(model.enabled()).thenReturn(false);errors=new ArrayList<>();
  pw=Playwright.create();browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
  page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1366,844));page.setDefaultTimeout(10000);page.onPageError(errors::add);
 }
 @AfterEach void close(){browser.close();pw.close();assertTrue(errors.isEmpty(),errors.toString());
  verify(model,never()).classify(anyString());assertEquals(0,payments.sandboxTransactionCount());}
 void plan(PhaseFourService.ActionPlan plan){page.navigate("http://localhost:8159/?action="+plan.id()+"#agent-workspace");}
 void language(String lang){page.getByTestId("language-"+lang).click();}
 void finished(){page.waitForFunction("()=>!document.querySelector('.content').hasAttribute('aria-busy')");}
 void capture(String name)throws Exception{Path dir=Path.of("docs/ui-ux-defect-fixes/after");Files.createDirectories(dir);
  page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(name+".png")));}
 void separate(Locator a,Locator b){
  BoundingBox x=a.boundingBox(),y=b.boundingBox();assertNotNull(x);assertNotNull(y);
  double width=Math.max(0,Math.min(x.x+x.width,y.x+y.width)-Math.max(x.x,y.x));
  double height=Math.max(0,Math.min(x.y+x.height,y.y+y.height)-Math.max(x.y,y.y));
  assertTrue(width*height<=1,"Unexpected overlap: "+a+" and "+b+" area="+width*height);
 }
 @Test void progressLabelsStayReadableForApprovalAndDelegatedAtEveryTargetSize()throws Exception{
  var tuition=payments.createTuitionPlan("BANK_A");plan(tuition);
  for(String permission:new String[]{"APPROVAL","DELEGATED"}){
   if(permission.equals("DELEGATED")){payments.emergencyStop();payments.setMode("DELEGATED");plan(payments.createLowRiskPlan(new BigDecimal("250000")));}
   for(int width:new int[]{360,390,1366}){page.setViewportSize(width,844);
    for(String lang:new String[]{"en","vi"}){language(lang);
     var label=page.locator(".payment-steps li:nth-child(2) .payment-step-label");label.scrollIntoViewIfNeeded();
     assertThat(label).hasText(permission.equals("APPROVAL")?(lang.equals("en")?"Approval":"Phê duyệt"):(lang.equals("en")?"Delegated":"Ủy quyền"));
     assertTrue((Boolean)label.evaluate("e=>e.scrollHeight<=e.clientHeight+1 && e.scrollWidth<=e.clientWidth+1 && getComputedStyle(e).borderRadius==='0px'"));
     assertTrue((Boolean)page.locator(".payment-steps").evaluate("e=>Array.from(e.querySelectorAll('.payment-step-label')).every(label=>{const range=document.createRange();range.selectNodeContents(label);const box=label.getBoundingClientRect();return Array.from(range.getClientRects()).every(r=>r.left>=box.left-1&&r.right<=box.right+1&&r.top>=box.top-1&&r.bottom<=box.bottom+1)})"));
     assertTrue((Boolean)page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
     capture("D1-"+permission+"-"+width+"-"+lang);
    }
   }
  }
 }
 @Test void notificationsUseLayoutSpaceAndNeverCoverPolicyApprovalOrChat()throws Exception{
  plan(payments.createTuitionPlan("BANK_A"));
  for(int width:new int[]{360,390,1366}){page.setViewportSize(width,844);
   for(String lang:new String[]{"en","vi"}){language(lang);
    page.getByTestId("emergency-stop").click();finished();
    var notice=page.locator("[data-workspace-notices] .notice");assertThat(notice).isVisible();
    separate(notice,page.getByTestId("global-permission-mode"));separate(notice,page.getByTestId("agent-state"));
    separate(notice,page.getByTestId("emergency-resume"));
    if(width<701)separate(notice,page.locator(".mobile-tabs"));
    capture("D2-header-"+width+"-"+lang);
    page.getByTestId("assistant-launcher").click();
    notice=page.locator("[data-assistant-notices] .notice");assertThat(notice).isVisible();
    separate(notice,page.getByTestId("assistant-permission-mode"));separate(notice,page.getByTestId("assistant-conversation-input"));
    separate(notice,page.getByTestId("assistant-send-message"));
    assertThat(page.getByTestId("assistant-conversation-input")).isInViewport();
    capture("D2-chat-"+width+"-"+lang);
    if(width<701){
     page.setViewportSize(width,450);
     assertThat(page.getByTestId("assistant-conversation-input")).isInViewport();
     assertThat(page.getByTestId("assistant-send-message")).isInViewport();
     separate(notice,page.getByTestId("assistant-conversation-input"));
     capture("D2-chat-short-"+width+"-"+lang);page.setViewportSize(width,844);
    }
    page.getByTestId("assistant-close").click();page.getByTestId("emergency-resume").click();finished();
    notice=page.locator("[data-workspace-notices] .notice");assertThat(notice).isVisible();
    page.getByTestId("approve-action").scrollIntoViewIfNeeded();separate(notice,page.getByTestId("approve-action"));
    assertThat(page.getByTestId("approve-action")).isEnabled();
    page.waitForFunction("()=>!document.querySelector('.notice.in-place-notice')",null,new Page.WaitForFunctionOptions().setTimeout(6000));
   }
  }
  // An error persists until dismissed, including when the panel opens/closes.
  page.route("**/agent/emergency-stop",route->route.abort());page.getByTestId("emergency-stop").click();finished();
  var error=page.locator("[data-workspace-notices] .request-error");assertThat(error).isVisible();
  error.locator("[data-dismiss-notice]").click();assertThat(error).hasCount(0);
 }
 @Test void internalTransferReviewUsesRecipientAndTransferGuidanceNotSchoolOrFx()throws Exception{
  var internal=payments.createLowRiskPlan(new BigDecimal("250000"));payments.emergencyStop();plan(internal);
  for(int width:new int[]{360,390,1366}){page.setViewportSize(width,844);
   for(String lang:new String[]{"en","vi"}){language(lang);
    assertThat(page.getByTestId("payment-reason-code")).hasText("AGENT PAUSED");
    assertThat(page.getByTestId("review-recipient-heading")).hasText(lang.equals("en")?"Internal transfer and recipient":"Chuyển nội bộ và người nhận");
    assertThat(page.getByTestId("review-beneficiary")).hasText("Emergency Fund");
    assertThat(page.getByTestId("internal-blocked-guidance")).hasText(lang.equals("en")?
     "Review the source account, recipient and policy status before creating a new transfer plan.":
     "Kiểm tra tài khoản nguồn, người nhận và trạng thái chính sách trước khi tạo kế hoạch chuyển tiền mới.");
    assertThat(page.locator("[data-testid='payment-blocked'] [data-return-to-comparison]")).hasCount(0);
    assertFalse(page.getByTestId("payment-safety-checks").innerText().matches("(?is).*(bill|quote|hóa đơn|báo giá).*"));
    assertFalse(page.getByTestId("cancel-action").locator("..").getAttribute("data-confirm-"+lang).matches("(?is).*(bill|quote|hóa đơn|báo giá).*"));
    assertFalse(page.locator(".payment-technical").textContent().matches("(?is).*(FX|báo giá).*"));
    assertFalse(page.getByTestId("payment-review").innerText().matches("(?is).*(School or education provider|Trường học hoặc nhà cung cấp giáo dục|FX markup|Phụ phí tỷ giá|Bill name|Tên hóa đơn).*"));
    page.getByTestId("payment-blocked").scrollIntoViewIfNeeded();capture("D3-guidance-"+width+"-"+lang);
    page.getByTestId("review-beneficiary").scrollIntoViewIfNeeded();capture("D3-internal-"+width+"-"+lang);
   }
  }
  payments.resumeAgent();plan(payments.createLowRiskPlan(new BigDecimal("250000")));
  assertFalse(page.getByTestId("approve-action").locator("..").getAttribute("data-confirm-en").matches("(?is).*(bill|quote).*"));
  // Tuition keeps its verified school information and its existing bill/quote guidance.
  payments.emergencyStop();plan(payments.createTuitionPlan("BANK_A"));language("en");
  assertThat(page.getByTestId("review-recipient-heading")).hasText("Bill and beneficiary");
  assertThat(page.getByTestId("payment-review")).containsText("School or education provider");
  assertThat(page.locator("[data-testid='payment-blocked'] [data-return-to-comparison]")).isVisible();
 }
}
