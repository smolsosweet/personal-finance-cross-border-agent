package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tc07_receipts;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","finbridge.llm.enabled=false"})
@AutoConfigureMockMvc
class ReceiptLinkIntegrationTest {
    @Autowired ReceiptLinkService receipts;
    @Autowired TransactionService transactions;
    @Autowired DemoDataService demo;
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;
    @MockitoBean GeminiDocumentExtractionService extraction;
    MockHttpSession session;
    LocalDate date;
    @BeforeEach void setup() { demo.resetAll(); session=new MockHttpSession(); date=LocalDate.now(); reset(extraction); }
    ReceiptLinkService.Draft draft(String merchant, LocalDate day) {
        return new ReceiptLinkService.Draft(merchant,new BigDecimal("50000"),"VND",day,"Cash","Food & Drinks","","");
    }
    String existing(String merchant,LocalDate day) {
        return transactions.recordManualTransaction(merchant,new BigDecimal("50000"),"VND",day,"Cash","Food & Drinks","");
    }
    String capability() { return receipts.issue(session,new byte[]{1,2,3}); }
    int count(String table) { return db.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class); }
    Map<String,Object> financial() {
        Map<String,Object> result=new TreeMap<>();
        for (String table:List.of("transactions","bank_events","financial_accounts","budgets","sandbox_transactions","sandbox_ledger_entries"))
            result.put(table,db.queryForList("SELECT * FROM "+table+" ORDER BY 1"));
        result.put("dashboard",transactions.dashboard()); result.put("budgetSummary",transactions.budgetSummary());
        return result;
    }
    @Test void exactMatchLinksOnceWithoutAnyFinancialChange() {
        String id=existing("TC07 Cafe",date);
        var before=financial(); String cap=capability();
        var result=receipts.save(session,cap,draft(" TC07   cAFE ",date));
        assertEquals("LINKED",result.state()); assertEquals(id,result.transactionId()); assertEquals(before,financial());
        assertEquals(1,count("receipt_evidence"));
        assertEquals(result,receipts.save(session,cap,draft("edited on replay",date)));
        assertEquals(id,receipts.save(session,capability(),draft("TC07 Cafe",date)).transactionId());
        assertEquals(1,count("receipt_evidence")); assertEquals(before,financial());
        assertTrue(countEvent("RECEIPT_LINK")>=1);
    }
    @Test void overrideDoubleClickReplayAndThirdIdenticalUploadAreSafe() throws Exception {
        String first=existing("TC07 Cafe",date); int before=count("transactions");
        var balances=db.queryForList("SELECT * FROM financial_accounts ORDER BY id");
        var total=db.queryForObject("SELECT SUM(amount) FROM transactions WHERE type='Expense' AND currency='VND'",BigDecimal.class);
        BigDecimal budgetBefore=(BigDecimal)transactions.budgetSummary().stream().filter(b->"Food & Drinks".equals(b.get("category"))).findFirst().orElseThrow().get("spent");
        String cap=capability(); assertEquals("LINKED",receipts.save(session,cap,draft("TC07 Cafe",date)).state());
        var pool=Executors.newFixedThreadPool(2);
        try {
            var a=pool.submit(()->receipts.differentPurchase(session,cap));
            var b=pool.submit(()->receipts.differentPurchase(session,cap));
            var x=a.get(10,TimeUnit.SECONDS); var y=b.get(10,TimeUnit.SECONDS);
            assertEquals("OVERRIDDEN",x.state()); assertEquals(x,y); assertNotEquals(first,x.transactionId());
            assertEquals(x,receipts.differentPurchase(session,cap));
            assertEquals(x,receipts.save(session,cap,draft("TC07 Cafe",date)));
            assertEquals(before+1,count("transactions")); assertEquals(2,count("receipt_evidence"));
            assertEquals(date.atStartOfDay(),db.queryForObject("SELECT occurred_at FROM transactions WHERE id=?",LocalDateTime.class,x.transactionId()));
            assertEquals(total.add(new BigDecimal("50000.00")),db.queryForObject("SELECT SUM(amount) FROM transactions WHERE type='Expense' AND currency='VND'",BigDecimal.class));
            assertEquals(balances,db.queryForList("SELECT * FROM financial_accounts ORDER BY id"),"Existing manual-entry balance behavior is unchanged");
            var third=receipts.save(session,capability(),draft("TC07 Cafe",date));
            assertEquals("ASK",third.state()); assertEquals(2,third.candidates().size()); assertEquals(before+1,count("transactions"));
            assertEquals(budgetBefore.add(new BigDecimal("50000.00")),transactions.budgetSummary().stream().filter(budget->"Food & Drinks".equals(budget.get("category"))).findFirst().orElseThrow().get("spent"));
            assertEquals(1,countEvent("RECEIPT_OVERRIDE")); assertEquals(1,countEvent("RECEIPT_ASK"));
        } finally { pool.shutdownNow(); }
    }
    @Test void forgedForeignExpiredAndResetCapabilitiesNeverWriteFinance() {
        String cap=capability(); var before=financial();
        assertEquals("REJECTED",receipts.differentPurchase(session,UUID.randomUUID().toString()).state());
        assertEquals("REJECTED",receipts.save(new MockHttpSession(),cap,draft("TC07 Cafe",date)).state());
        assertEquals("REJECTED",receipts.differentPurchase(new MockHttpSession(),cap).state());
        assertEquals("REJECTED",receipts.differentPurchase(session,cap).state(),"Cannot override before reviewing");
        db.update("UPDATE receipt_submissions SET expires_at=? WHERE capability=?",LocalDateTime.now().minusSeconds(1),cap);
        assertEquals("REJECTED",receipts.save(session,cap,draft("TC07 Cafe",date)).state());
        assertEquals("REJECTED",receipts.differentPurchase(session,cap).state());
        assertEquals(before,financial());
        demo.resetAll(); before=financial();
        assertEquals("REJECTED",receipts.differentPurchase(session,cap).state()); assertEquals(before,financial());
        assertEquals(1,countEvent("RECEIPT_REJECT"));
    }
    @Test void forgedCandidateNeverLinksOrWritesFinancialRows() {
        existing("Other Cafe",date); String cap=capability();
        assertEquals("ASK",receipts.save(session,cap,draft("TC07 Cafe",date)).state()); var before=financial();
        assertEquals("REJECTED",receipts.chooseExisting(session,cap,UUID.randomUUID().toString()).state());
        assertEquals(before,financial()); assertEquals(0,count("receipt_evidence")); assertEquals(1,countEvent("RECEIPT_REJECT"));
    }
    @Test void differentMerchantAsksAndExplicitCandidateChoiceLinks() {
        String id=existing("Other Cafe",date); var before=financial(); String cap=capability();
        assertEquals("ASK",receipts.save(session,cap,draft("TC07 Cafe",date)).state());
        assertEquals(before,financial());
        assertEquals("LINKED",receipts.chooseExisting(session,cap,id).state());
        assertEquals(id,receipts.chooseExisting(session,cap,id).transactionId()); assertEquals(before,financial());
    }
    @Test void multipleCandidatesAskWithoutWritingExpense() {
        existing("TC07 Cafe",date.minusDays(1)); existing("TC07 Cafe",date.plusDays(1)); var before=financial();
        var result=receipts.save(session,capability(),draft("TC07 Cafe",date));
        assertEquals("ASK",result.state()); assertEquals(2,result.candidates().size()); assertEquals(before,financial());
    }
    @Test void candidateWithDifferentEvidenceAsks() {
        existing("TC07 Cafe",date);
        assertEquals("LINKED",receipts.save(session,capability(),draft("TC07 Cafe",date)).state());
        var before=financial();
        assertEquals("ASK",receipts.save(session,capability(),draft("TC07 Cafe",date.plusDays(1))).state());
        assertEquals(before,financial()); assertEquals(1,count("receipt_evidence"));
    }
    @Test void outsideWindowCreatesNewExpenseAndReplaysOnce() {
        existing("TC07 Cafe",date.minusDays(30)); int before=count("transactions"); String cap=capability();
        var result=receipts.save(session,cap,draft("TC07 Cafe",date));
        assertEquals("CREATED",result.state()); assertEquals(before+1,count("transactions"));
        assertEquals(result,receipts.save(session,cap,draft("TC07 Cafe",date))); assertEquals(1,count("receipt_evidence"));
    }
    @Test void boundaryIsInclusiveAndNextDayIsOutside() {
        existing("TC07 Cafe",date.minusDays(3));
        assertEquals("LINKED",receipts.save(session,capability(),draft("TC07 Cafe",date)).state());
        existing("Other Test",date.plusDays(20));
        assertEquals("CREATED",receipts.save(session,capability(),draft("Other Test",date.plusDays(24))).state());
    }
    @ParameterizedTest @ValueSource(strings={"Income","Refund","Internal Transfer"})
    void nonExpensesNeverBecomeCandidates(String type) {
        String id=existing("TC07 Cafe",date.minusDays(1)); db.update("UPDATE transactions SET type=? WHERE id=?",type,id);
        int before=count("transactions");
        assertEquals("CREATED",receipts.save(session,capability(),draft("TC07 Cafe",date)).state());
        assertEquals(before+1,count("transactions"));
    }
    @Test void otherCurrencyNeverMatches() {
        String id=existing("TC07 Cafe",date.minusDays(1)); db.update("UPDATE transactions SET currency='CNY' WHERE id=?",id);
        assertEquals("CREATED",receipts.save(session,capability(),draft("TC07 Cafe",date)).state());
    }
    @Test void invalidReviewedFieldsRejectWithAuditAndNoExpense() {
        var before=financial();
        var invalid=new ReceiptLinkService.Draft("TC07 Cafe",BigDecimal.ZERO,"VND",date,"Cash","Food & Drinks","","");
        assertEquals("REJECTED",receipts.save(session,capability(),invalid).state()); assertEquals(before,financial());
        assertEquals(1,countEvent("RECEIPT_REJECT"));
    }
    @Test void manualBankAndSandboxDedupeAreUnchanged() {
        String a=existing("TC07 Cafe",date); assertEquals(a,existing("TC07 Cafe",date));
        var event=new TransactionService.BankEvent("BANK-A",date.atTime(9,0),"TC07 Bank","expense",new BigDecimal("12345"),"OUT","CHECKING",null,"Simulated Bank Event");
        String bank=transactions.ingest(event);
        assertEquals(bank,transactions.ingest(event));
        assertEquals(bank,transactions.ingest(new TransactionService.BankEvent("BANK-B",event.occurredAt(),event.merchant(),event.description(),event.amount(),"OUT","CHECKING",null,event.sourceLabel())));
        String receipt=transactions.recordSandboxPayment("SBOX-TC07","ACT-TC07","Q-TC07","BANK_A","School","Sandbox",new BigDecimal("10000"),"PAYER_VND",date.atTime(10,0));
        assertEquals(receipt,transactions.recordSandboxPayment("SBOX-TC07","ACT-TC07","Q-TC07","BANK_A","School","Sandbox",new BigDecimal("10000"),"PAYER_VND",date.atTime(10,0)));
    }
    @Test void capabilityOnlyIssuedForSuccessfulReceiptExtraction() throws Exception {
        when(extraction.extract(any(),eq("transaction"))).thenReturn(new DocumentExtractionService.Result(Map.of("merchant","TC07 Cafe"),0.9,"gemini"));
        var response=mvc.perform(multipart("/api/ai/extract-document").file(new MockMultipartFile("file","receipt.png","image/png",new byte[]{1,2,3})).param("kind","transaction").session(session))
                .andExpect(status().isOk()).andReturn().getResponse();
        String cap=response.getHeader("X-FinBridge-Receipt-Capability"); assertNotNull(cap);
        assertEquals(1,count("receipt_submissions")); assertEquals("ISSUED",db.queryForObject("SELECT state FROM receipt_submissions WHERE capability=?",String.class,cap));
        when(extraction.extract(any(),eq("transaction"))).thenThrow(new IllegalStateException("Provider offline"));
        mvc.perform(multipart("/api/ai/extract-document").file(new MockMultipartFile("file","receipt.png","image/png",new byte[]{1,2,3})).param("kind","transaction").session(session))
                .andExpect(status().isServiceUnavailable()).andExpect(header().doesNotExist("X-FinBridge-Receipt-Capability"));
        assertEquals(1,count("receipt_submissions"));
        mvc.perform(get("/").session(session)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("override_operation"))));
    }
    @Test void operationIsServerOnlyAndExpiresWithItsEvidence() throws Exception {
        existing("TC07 Cafe",date); String cap=capability();
        receipts.save(session,cap,draft("TC07 Cafe",date));
        String operation=db.queryForObject("SELECT override_operation FROM receipt_submissions WHERE capability=?",String.class,cap);
        var before=financial();
        mvc.perform(post("/transactions/receipt/different-purchase").session(session).param("receiptCapability",operation))
                .andExpect(status().is3xxRedirection());
        assertEquals(before,financial());
        mvc.perform(get("/").session(session)).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(operation))));
        db.update("UPDATE receipt_submissions SET expires_at=? WHERE capability=?",LocalDateTime.now().minusSeconds(1),cap);
        assertEquals("REJECTED",receipts.differentPurchase(session,cap).state()); assertEquals(before,financial());
    }
    @Test void offlineSampleIssuesRealCapabilityButIgnoresClientFieldsAndWritesNoExpense() throws Exception {
        var before=financial();
        var response=mvc.perform(post("/transactions/receipt/sample").session(session).param("merchant","Injected Merchant").param("amount","1").param("operationId","forged"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fields.merchant").value("Highlands Coffee"))
                .andExpect(jsonPath("$.fields.amount").value("130000")).andExpect(jsonPath("$.provider").value("synthetic")).andReturn().getResponse();
        assertNotNull(response.getHeader("X-FinBridge-Receipt-Capability")); assertEquals(before,financial());
        verifyNoInteractions(extraction);
    }
    @Test void protectedReferenceCollisionRejectsWrongEvidenceWithoutFinancialWrites() {
        transactions.recordManualTransaction("Other Merchant",new BigDecimal("51000"),"VND",date,"Cash","Food & Drinks","TC07-REFERENCE");
        var before=financial();
        var d=new ReceiptLinkService.Draft("TC07 Cafe",new BigDecimal("50000"),"VND",date,"Cash","Food & Drinks","TC07-REFERENCE","");
        assertEquals("REJECTED",receipts.save(session,capability(),d).state());
        assertEquals(before,financial()); assertEquals(0,count("receipt_evidence"));
    }
    int countEvent(String event) { return db.queryForObject("SELECT COUNT(*) FROM audit_log WHERE event_type=?",Integer.class,event); }
}