package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Opt-in Render timeout regression. Real browser/provider, no reset, quote refresh or payment actions. */
class HostedChatReadOnlyIT {
    private static final Path BUDGET = Path.of("target/hosted-chat-timeout-requests.txt");
    private String html;

    @Test void spendingAndBudgetQuestionsPreserveExistingFinancialState() throws Exception {
        String base = System.getProperty("hosted.demo.url");
        assertNotNull(base, "Supply the deployment URL explicitly");
        URI uri = URI.create(base);
        assertEquals("https", uri.getScheme());
        assertEquals("finbridge-shared-demo.onrender.com", uri.getHost());
        assertTrue(uri.getPath().isEmpty() || "/".equals(uri.getPath()));
        assertNull(uri.getQuery()); assertNull(uri.getUserInfo()); assertNull(uri.getFragment()); assertEquals(-1, uri.getPort());
        int used = Files.exists(BUDGET) ? Integer.parseInt(Files.readString(BUDGET).trim()) : 0;
        int count = Integer.getInteger("hosted.chat.cases", 2);
        assertTrue(count >= 1 && count <= 2);
        assertTrue(used + count <= 6, "Persisted limit: six submissions for this timeout investigation, no automatic retries");
        var errors = new ArrayList<String>();
        try (Playwright pw = Playwright.create();
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext context = browser.newContext()) {
            Page page = context.newPage(); page.setDefaultTimeout(60000); page.onPageError(errors::add);
            var response = page.navigate(base, new Page.NavigateOptions().setTimeout(90000));
            assertNotNull(response); assertEquals(200, response.status()); html = response.text();
            page.getByTestId("language-vi").click();
            List<String> before = financialState(page);
            page.getByTestId("assistant-launcher").click();
            String[][] cases = {{"Tháng này tôi chi nhiều nhất vào đâu?", "EXPLAIN_SPENDING_SUMMARY"},
                    {"Show my configured budgets for this month.", "EXPLAIN_BUDGET_STATUS"}};
            for (int i = 0; i < count; i++) {
                String prior = latest(page, "AI REQUEST FINISHED", 2);
                Files.writeString(BUDGET, Integer.toString(++used));
                long start = System.nanoTime();
                page.getByTestId("assistant-conversation-input").fill(cases[i][0]);
                var answer = page.waitForResponse(r -> r.status() == 200 && r.url().startsWith(base)
                                && r.request().method().equals("GET"),
                        () -> page.getByTestId("assistant-send-message").click());
                html = answer.text();
                page.waitForFunction("() => !document.querySelector('.content').hasAttribute('aria-busy')");
                String request = latest(page, "AI REQUEST FINISHED", 2);
                boolean schema = !request.equals(prior) && request.equals(latest(page, "INTENT CLASSIFIED", 2));
                String actual = schema ? latest(page, "INTENT CLASSIFIED", 4) : latest(page, "AI REQUEST FINISHED", 4);
                boolean unchanged = before.equals(financialState(page));
                System.out.printf("HOSTED_CHAT case=%d expected=%s actual=%s strict_parser_audit=%s latency_ms=%d financial_ui_unchanged=%s%n",
                        used, cases[i][1], actual, schema, (System.nanoTime() - start) / 1_000_000, unchanged);
                assertTrue(unchanged, "Balances, existing plans, receipts and transaction list must remain unchanged");
                assertTrue(schema, "Provider fallback is not a successful model response: " + actual);
                assertEquals(cases[i][1], actual);
            }
            assertTrue(errors.isEmpty(), "JavaScript errors: " + errors);
        }
    }

    private List<String> financialState(Page page) {
        var result = new ArrayList<String>();
        for (var row : page.locator(".sandbox-account").all())
            result.add(row.locator("small").textContent() + "=" + row.locator("b").textContent());
        for (var row : page.locator(".connected-payment-card").all())
            result.add(row.getAttribute("data-testid") + "=" + row.getAttribute("data-balance"));
        result.add(page.getByTestId("total-personal-balance").locator("strong").textContent());
        for (var row : page.locator("[data-testid=latest-action], [data-testid=latest-receipt]").all())
            result.add(row.getAttribute("data-testid") + ":" + row.getAttribute("data-action-id")
                    + ":" + row.getAttribute("data-status") + ":" + row.getAttribute("data-transaction-id"));
        for (var row : page.getByTestId("payment-history").locator("[data-open-payment-plan]").all())
            result.add(row.getAttribute("data-action-id") + ":" + row.locator("[data-payment-status]").getAttribute("data-status"));
        for (var row : page.getByTestId("transaction-row").all())
            result.add(row.getAttribute("data-transaction-id") + ":" + row.getAttribute("data-review-status")
                    + ":" + row.getAttribute("data-type") + ":" + row.getAttribute("data-category") + ":" + row.getAttribute("data-amount"));
        return result;
    }

    private String latest(Page page, String event, int cell) {
        // Canonical enum from server HTML, before UI translation; correlate this exact request.
        return (String) page.evaluate("arg => { const doc=new DOMParser().parseFromString(arg.html,'text/html'); for(const row of doc.querySelectorAll('[data-testid=demo-audit-log] tbody tr')) { const cells=row.querySelectorAll('td'); if(cells[1].textContent.trim()===arg.event)return cells[arg.cell].textContent.trim(); } return 'NONE'; }",
                Map.of("html", html, "event", event, "cell", cell));
    }
}
