package com.example.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:finance_workspace_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class FinanceWorkspaceIntegrationTest {
    @Autowired FinanceWorkspaceService workspace;
    @Autowired DemoDataService demoData;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void reset() {
        demoData.resetAll();
    }

    @Test
    void summaryUsesUnifiedLedgerAndReservesOnlyActivePlans() {
        var summary = workspace.summary();

        assertMoney("315000000.00", summary.totalBalance());
        assertMoney("8300000.00", summary.reservedNext30Days());
        assertMoney("303700000.00", summary.availableToAllocate());
        assertEquals(3, summary.activePlanCount());

        workspace.completePlan("PLAN-RENT");
        assertMoney("300000.00", workspace.summary().reservedNext30Days());
        assertMoney("311700000.00", workspace.summary().availableToAllocate());

        workspace.reopenPlan("PLAN-RENT");
        assertMoney("8300000.00", workspace.summary().reservedNext30Days());
        assertMoney("303700000.00", workspace.summary().availableToAllocate());
    }

    @Test
    void manualMoneySourcesAreEditableWhileConnectedBalancesStayReadOnly() {
        String id = workspace.addManualAccount("Cash wallet", "Cash", "CASH", null,
                new BigDecimal("2000000"));
        assertMoney("317000000.00", workspace.summary().totalBalance());
        assertEquals("CASH", db.queryForObject(
                "SELECT source_type FROM financial_accounts WHERE id=?", String.class, id));

        workspace.updateManualAccount(id, "Travel cash", "Cash", "CASH", null,
                new BigDecimal("1500000"));
        assertMoney("316500000.00", workspace.summary().totalBalance());

        var error = assertThrows(IllegalStateException.class, () ->
                workspace.updateManualAccount("CHECKING", "Changed", "Demo Bank", "CHECKING",
                        "•••• 1106", new BigDecimal("1")));
        assertTrue(error.getMessage().contains("bank events"));
    }

    @Test
    void recurringPlanProjectsOccurrencesAndArchiveReleasesReserve() {
        String id = workspace.addPlan("Weekly groceries", "RECURRING_BILL", "Groceries",
                new BigDecimal("1000000"), "WEEKLY", LocalDate.now(), "CHECKING",
                true, "Weekly allowance");

        assertMoney("13300000.00", workspace.summary().reservedNext30Days());
        var plan = workspace.plans().stream().filter(row -> id.equals(row.get("id"))).findFirst().orElseThrow();
        assertEquals(5, ((Number) plan.get("projected_occurrences")).intValue());
        assertMoney("5000000.00", (BigDecimal) plan.get("projected_30_day_amount"));

        workspace.archivePlan(id);
        assertMoney("8300000.00", workspace.summary().reservedNext30Days());
    }

    @Test
    void monthlyBudgetIsSourceOfTruthForWeeklyGuide() {
        workspace.updateBudget("Food & Drinks", new BigDecimal("4345000"));

        assertMoney("8645000.00", workspace.summary().monthlyBudget());
        assertMoney("1989643.00", workspace.summary().weeklyGuide());
    }

    @Test
    void activeSpendingCategoryCanReceiveOneBudgetOnly() {
        workspace.addBudget("Groceries", new BigDecimal("1800000"));

        assertTrue(workspace.budgetCategories().stream().noneMatch("Groceries"::equals));
        assertMoney("8100000.00", workspace.summary().monthlyBudget());
        assertThrows(IllegalStateException.class,
                () -> workspace.addBudget("Groceries", new BigDecimal("2000000")));
        assertThrows(IllegalArgumentException.class,
                () -> workspace.addBudget("Income", new BigDecimal("2000000")));
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
