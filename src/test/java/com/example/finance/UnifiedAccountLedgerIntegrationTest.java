package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:unified_accounts;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class UnifiedAccountLedgerIntegrationTest {
    @Autowired DemoDataService demo; @Autowired AccountLedgerService ledger;
    @Autowired PhaseFourService payments; @Autowired TransactionService transactions;
    @Autowired FinanceWorkspaceService workspace; @Autowired GlobalAssistantQueries queries;
    @Autowired JdbcTemplate db; @Autowired PersonalFinanceInsights insights;
    @BeforeEach void reset(){demo.resetAll();}
    static void money(String expected,BigDecimal value){assertEquals(0,new BigDecimal(expected).compareTo(value));}
    @Test void allScreensHaveTheSameSixOwnedSourcesAndNeverCountRecipientMoney(){
        assertEquals(6,workspace.accounts().size());assertEquals(6,queries.accounts().size());
        assertEquals(2,queries.namedAccounts("Hiện Bank A còn bao nhiêu?").size());
        money("315000000",workspace.summary().totalBalance());
        db.update("UPDATE financial_accounts SET balance=900000000 WHERE account_scope='SYSTEM'");
        money("315000000",workspace.summary().totalBalance());
        assertFalse(queries.balances(queries.accounts(),false).contains("900000000"));
        money("100000000",payments.paymentSource("PAYER_VND").balance());
        assertEquals("VIEW",db.queryForObject("SELECT table_type FROM information_schema.tables WHERE table_name='sandbox_accounts'",String.class));
    }
    @Test void bankEventsAndPaymentsUpdateOneBalanceAndReceiptIsHistorical(){
        var event=new TransactionService.BankEvent("UNIFIED-COFFEE",LocalDateTime.now(),"Highlands Coffee","Coffee",new BigDecimal("85000"),"OUT","CHECKING",null,"Simulated Bank Event");
        String tx=transactions.ingest(event);assertEquals(tx,transactions.ingest(event));
        money("99915000",payments.paymentSource("PAYER_VND").balance());
        money("314915000",workspace.summary().totalBalance());
        var plan=payments.createTuitionPlan("BANK_A");
        money("99915000",ledger.balance("CHECKING"));assertNull(payments.receiptForAction(plan.id()));
        var receipt=payments.approveAndExecute(plan.id());assertNotNull(receipt);
        money("29154200",receipt.vndBalanceAfter());money("244154200",workspace.summary().totalBalance());
        assertEquals(receipt,payments.approveAndExecute(plan.id()));money("29154200",ledger.balance("CHECKING"));
        assertTrue(queries.balances(List.of(payments.paymentSource("PAYER_VND")),false).contains("29154200.00"));
        transactions.ingest(new TransactionService.BankEvent("AFTER-PAYMENT",LocalDateTime.now().plusMinutes(1),"Employer","Salary",new BigDecimal("1000000"),"IN","CHECKING",null,"Simulated Bank Event"));
        money("30154200",payments.paymentSource("PAYER_VND").balance());
        money("29154200",payments.receiptForAction(plan.id()).vndBalanceAfter());
        assertEquals(1,payments.sandboxTransactionCount());
    }
    @Test void transfersAcrossOwnedSourcesConserveMoneyAndNeverAddIncomeOrExpense(){
        var before=transactions.dashboard();
        transactions.ingest(new TransactionService.BankEvent("TCB-SAVINGS",LocalDateTime.now(),"Own account","Move funds",new BigDecimal("300000"),"OUT","TCB_VND","SAVINGS","Simulated Bank Event"));
        money("44700000",ledger.balance("TCB_VND"));money("1300000",ledger.balance("SAVINGS"));
        money("315000000",workspace.summary().totalBalance());
        assertEquals(before.get("income"),transactions.dashboard().get("income"));
        assertEquals(before.get("expenses"),transactions.dashboard().get("expenses"));
        payments.setMode("DELEGATED");var plan=payments.createLowRiskPlan(new BigDecimal("250000"));
        assertEquals("COMPLETED",plan.status());money("1550000",ledger.balance("SAVINGS"));
        money("315000000",workspace.summary().totalBalance());
    }
    @Test void manualCashIsSharedButHasNoPaymentPermissionAndArchiveRemovesItEverywhere(){
        String id=workspace.addManualAccount("Travel cash","Cash","CASH",null,new BigDecimal("2000000"));
        assertEquals(7,queries.accounts().size());money("317000000",workspace.summary().totalBalance());
        assertFalse(payments.paymentSource(id).ready());
        assertTrue(queries.balances(queries.accounts(),false).contains("manually maintained"));
        assertThrows(IllegalArgumentException.class,()->payments.selectPaymentSource(id));
        workspace.updateManualAccount(id,"Travel cash","Cash","CASH",null,new BigDecimal("1500000"));
        money("1500000",payments.paymentSource(id).balance());money("316500000",workspace.summary().totalBalance());
        workspace.archiveManualAccount(id);assertEquals(6,queries.accounts().size());money("315000000",workspace.summary().totalBalance());
    }
    @Test void projectionsReserveOnlyPlansFundedByTheChosenAccount(){
        workspace.addPlan("VCB rent","RECURRING_BILL","Housing",new BigDecimal("1000000"),"ONCE",java.time.LocalDate.now(),"VCB_VND",true,"");
        String projection=insights.tuitionProjection(payments,1,"VCB_VND",ModelConversationContext.Channel.VCB,false);
        assertTrue(projection.contains("Known reserved planner commitments for 30 days: 1000000.00 VND"),projection);
        assertFalse(projection.contains("9300000.00"));
    }
    @Test void disconnectedSourceHasClearReasonAndCannotUseAPreviouslyCreatedPlan(){
        var plan=payments.createTuitionPlan("BANK_A");
        db.update("UPDATE financial_accounts SET connection_status='DISCONNECTED' WHERE id='CHECKING'");
        var source=payments.paymentSource("PAYER_VND");assertFalse(source.ready());
        assertTrue(source.eligibilityReason().contains("disconnected"));
        assertNull(payments.approveAndExecute(plan.id()));assertEquals(0,payments.sandboxTransactionCount());
        money("100000000",ledger.balance("PAYER_VND"));
    }
    @Test void installingPaymentCapabilitiesDoesNotOverwriteExistingPersonalBankEvents(){
        transactions.ingest(new TransactionService.BankEvent("BEFORE-CAPABILITIES",LocalDateTime.now(),"Highlands Coffee","Coffee",new BigDecimal("85000"),"OUT","CHECKING",null,"Simulated Bank Event"));
        db.update("DELETE FROM agent_policy");payments.seedIfEmpty();
        money("99915000",payments.paymentSource("PAYER_VND").balance());
        money("314915000",workspace.summary().totalBalance());
    }
    @Test void incompleteStartupDoesNotSilentlyEraseAnExistingReceipt(){
        var plan=payments.createTuitionPlan("BANK_A");var receipt=payments.approveAndExecute(plan.id());
        db.update("DELETE FROM financial_accounts WHERE id='SCHOOL_USD'");
        assertThrows(IllegalStateException.class,payments::seedIfEmpty);
        assertEquals(receipt,payments.receiptForAction(plan.id()));money("29239200",ledger.balance("PAYER_VND"));
    }
    @Test void simultaneousBankEventAndApprovedPaymentDoNotLoseEitherBalanceUpdate() throws Exception {
        var plan=payments.createTuitionPlan("BANK_A");
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var start=new java.util.concurrent.CyclicBarrier(2);
        try {
            var bank=pool.submit(()->{start.await();return transactions.ingest(new TransactionService.BankEvent("CONCURRENT",LocalDateTime.now(),"Highlands Coffee","Coffee",new BigDecimal("85000"),"OUT","CHECKING",null,"Simulated Bank Event"));});
            var payment=pool.submit(()->{start.await();return payments.approveAndExecute(plan.id());});
            assertNotNull(bank.get(10,java.util.concurrent.TimeUnit.SECONDS));
            assertNotNull(payment.get(10,java.util.concurrent.TimeUnit.SECONDS));
            money("29154200",ledger.balance("CHECKING"));money("244154200",workspace.summary().totalBalance());
            assertEquals(1,payments.sandboxTransactionCount());
        } finally {pool.shutdownNow();}
    }
    @Test void threeResetsRestoreEveryViewWithoutDuplicateSources(){
        for(int i=0;i<3;i++){
            payments.setMode("DELEGATED");payments.createLowRiskPlan(new BigDecimal("250000"));
            workspace.addManualAccount("Cash","Cash","CASH",null,BigDecimal.TEN);demo.resetAll();
            assertEquals(6,workspace.accounts().size());assertEquals(6,queries.accounts().size());
            money("315000000",workspace.summary().totalBalance());money("100000000",ledger.balance("PAYER_VND"));
            assertEquals(0,payments.sandboxTransactionCount());
        }
    }
    @Test void legacyMigrationCombinesChangesOncePreservesHistoryAndSurvivesSchemaReplay(){
        var ds=new DriverManagerDataSource("jdbc:h2:mem:legacy_unified;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        var scripts=new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));scripts.execute(ds);
        var old=new JdbcTemplate(ds);
        old.update("INSERT INTO financial_accounts(id,owner_profile_id,account_name,currency,balance) VALUES ('CHECKING',1,'Everyday','VND',99915000),('SAVINGS',1,'Emergency fund','VND',1000000)");
        old.update("INSERT INTO sandbox_accounts VALUES ('PAYER_VND','Bank A Everyday','VND',29239200),('EMERGENCY_VND','Emergency Fund','VND',250000),('SCHOOL_CNY','School','CNY',20000)");
        old.update("INSERT INTO payment_source_accounts(account_id,institution,account_type,masked_number,connection_status,verification_status,cross_border_enabled,selected,display_order) VALUES ('PAYER_VND','Bank A','Everyday account','2048','DISCONNECTED','VERIFIED',TRUE,TRUE,1)");
        old.update("""
                INSERT INTO sandbox_transactions
                (id,action_id,idempotency_key,channel_id,quote_id,recipient,vnd_balance_before,vnd_debit,
                 conversion_vnd,fee_deduction_vnd,vnd_balance_after,cny_balance_before,cny_credit,cny_balance_after,
                 rate_vnd_per_cny,status,created_at,source_account_id)
                VALUES ('LEGACY-RECEIPT','LEGACY-ACT','LEGACY-IDEMP','BANK_A','OLD-QUOTE','SCHOOL',100000000,
                        70760800,70400000,360800,29239200,0,20000,20000,3520,'COMPLETED',CURRENT_TIMESTAMP,'PAYER_VND')
                """);
        var immutable=old.queryForList("SELECT * FROM sandbox_transactions");
        var service=new AccountLedgerService(old);service.migrateLegacyBalances();
        money("29154200",service.balance("PAYER_VND"));money("1250000",service.balance("SAVINGS"));
        scripts.execute(ds);service.migrateLegacyBalances();
        money("29154200",service.balance("PAYER_VND"));money("1250000",service.balance("SAVINGS"));
        money("20000",service.balance("SCHOOL_CNY"));
        assertEquals(immutable,old.queryForList("SELECT * FROM sandbox_transactions"));
        assertEquals("DISCONNECTED",old.queryForObject("SELECT connection_status FROM financial_accounts WHERE id='CHECKING'",String.class));
        assertEquals(3,old.queryForObject("SELECT COUNT(*) FROM financial_accounts",Integer.class));
    }
}
