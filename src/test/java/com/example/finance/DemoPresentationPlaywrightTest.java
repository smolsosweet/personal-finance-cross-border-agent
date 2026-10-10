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
    "server.port=8154","spring.datasource.url=jdbc:h2:mem:competition_view;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","finbridge.llm.enabled=false"})
class DemoPresentationPlaywrightTest {
    @Autowired DemoDataService demo;
    @Autowired CrossBorderService border;
    @Autowired DemoPresentationScope scope;
    @MockitoBean LlmIntentClient llm;
    @Test void defaultCompetitionHasThreePrdChannelsAndHonestLabelsInBothLanguages() throws Exception {
        demo.resetAll(); when(llm.enabled()).thenReturn(false);
        var full=border.rankedQuotes(); assertEquals(6,full.size(),"Extension backend data is retained");
        var projected=scope.quotes(full);
        assertEquals(3,projected.size()); assertEquals(2,projected.stream().filter(CrossBorderService.ChannelQuote::eligible).count());
        assertEquals(full.stream().filter(q->DemoPresentationScope.COMPETITION_CHANNELS.contains(q.channelId())).toList(),projected,"Projection preserves the original ranking and quote objects");
        assertEquals(6,new DemoPresentationScope("extension").quotes(full).size());
        Path dir=Files.createDirectories(Path.of("target","demo-presentation",UUID.randomUUID().toString()));
        try (var pw=Playwright.create();var browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true))) {
            var page=browser.newPage();
            page.navigate("http://localhost:8154");
            for (String language:List.of("en","vi")) {
                page.getByTestId("language-"+language).click();
                page.locator("[data-tab='dashboard']:visible").first().click();
                page.locator("[data-dashboard-view='accounts']:visible").first().click();
                assertThat(page.getByTestId("money-source-CHECKING").locator(".source-type")).hasText(language.equals("vi")?"Nguồn mô phỏng":"Simulated source");
                page.locator("[data-tab='student']:visible").first().click();
                assertEquals(3,page.locator("[data-testid^='channel-']").count());
                for(String channel:DemoPresentationScope.COMPETITION_CHANNELS) assertThat(page.getByTestId("channel-"+channel)).isVisible();
                assertEquals(0,page.locator("[data-testid='channel-VCB'],[data-testid='channel-TCB'],[data-testid='channel-MOMO']").count());
                assertEquals(0,page.getByTestId("channel-BANK_B").locator("form").count(),"Bank B remains reference-only");
                assertThat(page.getByTestId("channel-BANK_B")).containsText(language.equals("vi")?"Chỉ tham khảo: chưa liên kết nguồn Bank B trong mô phỏng":"Reference only: no linked Bank B source in the simulation");
                page.locator("[data-open-dialog='add-student-expense']:visible").first().click();
                assertEquals(List.of("China"),page.locator("#add-student-expense [data-corridor-select] option").evaluateAll("options=>options.map(o=>o.value)"));
                page.locator("#add-student-expense [data-close-dialog]").click();
                for(int width:new int[]{390,1440}) {
                    page.setViewportSize(width,900);
                    assertTrue((Boolean)page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
                    page.locator(".channel-comparison-section").screenshot(new Locator.ScreenshotOptions().setPath(dir.resolve(language+"-"+width+".png")));
                }
            }
        }
        verify(llm,never()).classify(anyString());
    }
}