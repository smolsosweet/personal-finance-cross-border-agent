package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:payment_source_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class PaymentSourceIntegrationTest {
    @Autowired PhaseFourService phaseFour;
    @Autowired DemoDataService demoData;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void reset() {
        demoData.resetAll();
    }

    @Test
    void allPersonalAccountsAreExposedAndSeparatedByEligibility() {
        var accounts = phaseFour.paymentSourceAccounts();

        assertEquals(5, accounts.size());
        assertEquals(3, accounts.stream().filter(PhaseFourService.PaymentSourceAccount::ready).count());
        assertEquals(2, accounts.stream().filter(account -> !account.ready()).count());
        assertEquals(PhaseFourService.PAYER, phaseFour.selectedPaymentSource().accountId());
    }

    @Test
    void selectedSourceIsFrozenIntoPlanAndDebitedEvenAfterUiSelectionChanges() {
        phaseFour.selectPaymentSource("VCB_VND");
        var plan = phaseFour.createTuitionPlan("BANK_A", "VCB_VND");
        phaseFour.selectPaymentSource(PhaseFourService.PAYER);

        var receipt = phaseFour.approveAndExecute(plan.id());

        assertNotNull(receipt);
        assertEquals("VCB_VND", plan.sourceAccountId());
        assertEquals("VCB_VND", receipt.sourceAccountId());
        assertMoney("11239200.00", balance("VCB_VND"));
        assertMoney("100000000.00", balance(PhaseFourService.PAYER));
        assertEquals("VCB_VND", db.queryForObject(
                "SELECT source_account_id FROM sandbox_transactions WHERE id=?",
                String.class, receipt.transactionId()));
    }

    @Test
    void unsupportedPersonalAccountCannotBeSelectedOrUsed() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> phaseFour.selectPaymentSource("MOMO_VND"));
        assertTrue(error.getMessage().contains("not eligible"));

        var plan = phaseFour.createTuitionPlan("BANK_A", "MOMO_VND");
        assertEquals("BLOCKED", plan.status());
        assertEquals("SOURCE ACCOUNT NOT ELIGIBLE", db.queryForObject("""
                SELECT reason_code FROM audit_log
                WHERE reference_id=? AND event_type='POLICY_CHECKED'
                ORDER BY occurred_at DESC FETCH FIRST 1 ROWS ONLY
                """, String.class, plan.id()));
    }

    private BigDecimal balance(String id) {
        return db.queryForObject("SELECT balance FROM sandbox_accounts WHERE id=?", BigDecimal.class, id);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
