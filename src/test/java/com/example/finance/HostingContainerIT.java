package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Opt-in container smoke. Uses an owned ephemeral container; model disabled, no approval. */
class HostingContainerIT {
    @Test void actualContainerRestartRestoresSeedAndRunsAsNonRoot() throws Exception {
        String name="finbridge-hosting-check-"+UUID.randomUUID().toString().substring(0,8);
        boolean started=false;
        try{
            command("docker","run","-d","--rm","--name",name,"-p","127.0.0.1:8125:8080",
                    "-e","FINBRIDGE_LLM_ENABLED=false","finbridge-shared-demo:context-stability");started=true;
            awaitReady();
            assertEquals("finbridge",command("docker","inspect","--format","{{.Config.User}}",name).trim());
            try(Playwright pw=Playwright.create();Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true))){
                Page page=browser.newPage();page.navigate("http://localhost:8125");page.getByTestId("language-en").click();
                assertThat(page.getByTestId("shared-demo-notice")).containsText("Restart restores the seed data");
                assertThat(page.getByTestId("tab-agent")).isHidden();
                page.getByTestId("tab-student").click();page.getByTestId("plan-BANK_A").click();
                page.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')");
                assertThat(page.getByTestId("latest-action")).hasAttribute("data-status","AWAITING_APPROVAL");
                assertThat(page.getByTestId("latest-receipt")).isHidden();
                command("docker","restart",name);awaitReady();page.navigate("http://localhost:8125");
                assertThat(page.getByTestId("tab-agent")).isHidden();assertThat(page.getByTestId("latest-action")).isHidden();
                assertThat(page.getByTestId("latest-receipt")).isHidden();
                page.getByTestId("tab-student").click();assertThat(page.getByTestId("plan-BANK_A")).isVisible();
                page.getByTestId("language-vi").click();assertThat(page.getByTestId("shared-demo-notice")).containsText("Restart đưa dữ liệu về seed");
                System.out.println("HOSTING_CONTAINER non_root=PASS startup=PASS awaiting_approval=PASS restart_seed=PASS bilingual_notice=PASS model_calls=0 approvals=0");
            }
        }finally{if(started)command("docker","stop",name);}
    }
    private void awaitReady() throws Exception {
        HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        long deadline=System.nanoTime()+Duration.ofSeconds(45).toNanos();
        while(System.nanoTime()<deadline){
            try{var response=client.send(HttpRequest.newBuilder(URI.create("http://localhost:8125/")).timeout(Duration.ofSeconds(2)).GET().build(),HttpResponse.BodyHandlers.ofString());
                if(response.statusCode()==200&&response.body().contains("shared-demo-notice"))return;
            }catch(java.io.IOException ignored){}
            Thread.sleep(250);
        }
        fail("Owned hosting container did not become ready on port 8125 within 45 seconds");
    }
    private String command(String... args) throws Exception {
        Process process=new ProcessBuilder(args).redirectErrorStream(true).start();
        assertTrue(process.waitFor(30,TimeUnit.SECONDS),"Docker command timed out");
        String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0,process.exitValue(),output);return output;
    }
}
