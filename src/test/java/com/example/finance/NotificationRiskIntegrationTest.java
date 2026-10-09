package com.example.finance;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:notice_risk;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class NotificationRiskIntegrationTest {
    @Autowired DemoDataService demo;
    @Autowired TransactionService transactions;
    @Autowired FinanceWorkspaceService workspace;
    @Autowired JdbcTemplate db;
    @BeforeEach void reset(){demo.resetAll();}
    Map<String,Object> budget(){return transactions.proactiveFeed().stream()
            .filter(i->"Budget progress".equals(i.get("title"))).findFirst().orElse(null);}
    @Test void healthySeedHasNoLowUsageBudgetOrHealthyBufferNotification(){
        assertNull(budget());
        var review=transactions.proactiveFeed().stream().filter(i->"Transactions need your input".equals(i.get("title"))).findFirst().orElseThrow();
        assertEquals("MEDIUM",review.get("priority"));assertEquals("ACTION",review.get("notificationKind"));
        assertEquals("MEDIUM",workspace.attentionItems(4).getFirst().get("priority"));
        assertFalse(transactions.proactiveFeed().stream().anyMatch(i->i.get("title").toString().startsWith("Safety buffer")));
        var reservation=workspace.attentionItems(0).stream().filter(i->"Upcoming money is reserved".equals(i.get("title"))).findFirst().orElseThrow();
        assertEquals("INFO",reservation.get("priority"));assertEquals("UPDATE",reservation.get("notificationKind"));
    }
    @Test void budgetThresholdUsesActualAmountsNotRoundedDisplayPercent(){
        BigDecimal spent=(BigDecimal)transactions.budgetSummary().stream().filter(i->"Utilities".equals(i.get("category"))).findFirst().orElseThrow().get("spent");
        BigDecimal threshold=spent.divide(new BigDecimal("0.8"));
        db.update("UPDATE budgets SET monthly_limit=? WHERE category='Utilities'",threshold.add(new BigDecimal("0.01")));
        assertNull(budget());
        db.update("UPDATE budgets SET monthly_limit=? WHERE category='Utilities'",threshold);
        assertEquals("MEDIUM",budget().get("priority"));assertEquals("UPDATE",budget().get("notificationKind"));
        db.update("UPDATE budgets SET monthly_limit=? WHERE category='Utilities'",spent);
        assertEquals("HIGH",budget().get("priority"));assertEquals("ACTION",budget().get("notificationKind"));
        db.update("UPDATE budgets SET monthly_limit=? WHERE category='Utilities'",spent.subtract(new BigDecimal("0.01")));
        assertEquals("HIGH",budget().get("priority"));
    }
    @Test void bufferAlertsOnlyBelowThresholdAndDoesNotChangeMoney(){
        db.update("UPDATE financial_accounts SET balance=3000000 WHERE id='CHECKING'");
        assertFalse(transactions.proactiveFeed().stream().anyMatch(i->"Safety buffer needs attention".equals(i.get("title"))));
        db.update("UPDATE financial_accounts SET balance=2999999 WHERE id='CHECKING'");
        var before=PersonalFinanceAiIntegrationTest.snapshot(db);
        var alert=transactions.proactiveFeed().stream().filter(i->"Safety buffer needs attention".equals(i.get("title"))).findFirst().orElseThrow();
        assertEquals("HIGH",alert.get("priority"));assertEquals("ACTION",alert.get("notificationKind"));
        assertTrue(alert.get("message").toString().contains("1 VND below"));assertEquals("#accounts",alert.get("href"));
        assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
    }
}
