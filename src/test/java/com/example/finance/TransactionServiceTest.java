package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:phase1_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class TransactionServiceTest {
    @Autowired TransactionService service;
    @Autowired JdbcTemplate db;
    @BeforeEach void reset() { service.reset(); }

    @Test void recognizesAllFourTypes() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals("Expense", TransactionService.normalize(new TransactionService.BankEvent("1",now,"Highlands","Card purchase",new BigDecimal("85000")," out ","CHECKING",null,"test")).type());
        assertEquals("Income", TransactionService.normalize(new TransactionService.BankEvent("2",now,"Employer","Salary",new BigDecimal("900000"),"IN","CHECKING",null,"test")).type());
        assertEquals("Internal Transfer", TransactionService.normalize(new TransactionService.BankEvent("3",now,"Own Account","Move savings",new BigDecimal("200000"),"OUT","CHECKING","SAVINGS","test")).type());
        assertEquals("Refund", TransactionService.normalize(new TransactionService.BankEvent("4",now,"Highlands","Refund",new BigDecimal("85000"),"IN","CHECKING",null,"test")).type());
    }

    @Test void normalizesAndRejectsInvalidAmounts() {
        var event = new TransactionService.BankEvent("1", LocalDateTime.now(),"  Highlands   Coffee  ","  Card purchase  ",new BigDecimal("85000")," out ","CHECKING",null,"test");
        var normalized = TransactionService.normalize(event);
        assertEquals("Highlands Coffee", normalized.merchant());
        assertEquals("Card purchase", normalized.description());
        assertEquals(new BigDecimal("85000.00"), normalized.amount());
        assertEquals("VND", normalized.currency());
        assertEquals("OUT", normalized.direction());
        assertThrows(IllegalArgumentException.class, () -> TransactionService.normalize(new TransactionService.BankEvent("2",LocalDateTime.now(),"X","",BigDecimal.ZERO,"IN",null,null,"test")));
    }

    @Test void eventUpdatesAccountsWithoutDoubleCountingTransfer() {
        BigDecimal checking = db.queryForObject("SELECT balance FROM financial_accounts WHERE id='CHECKING'",BigDecimal.class);
        BigDecimal savings = db.queryForObject("SELECT balance FROM financial_accounts WHERE id='SAVINGS'",BigDecimal.class);
        service.simulate("transfer");
        assertEquals(0, checking.subtract(new BigDecimal("200000")).compareTo(db.queryForObject("SELECT balance FROM financial_accounts WHERE id='CHECKING'",BigDecimal.class)));
        assertEquals(0, savings.add(new BigDecimal("200000")).compareTo(db.queryForObject("SELECT balance FROM financial_accounts WHERE id='SAVINGS'",BigDecimal.class)));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM transactions WHERE type='Internal Transfer' AND source_label='Simulated Bank Event' AND direction='IN'",Integer.class));
    }

    @Test void duplicateFingerprintDoesNotCreateAnotherTransaction() {
        LocalDateTime at = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        var first = new TransactionService.BankEvent("A",at,"Highlands Coffee","Card",new BigDecimal("85000"),"OUT","CHECKING",null,"Simulated Bank Event");
        var second = new TransactionService.BankEvent("B",at,"  Highlands Coffee  ","Card",new BigDecimal("85000"),"OUT","CHECKING",null,"Simulated Bank Event");
        int before = service.eventCount();
        String id = service.ingest(first);
        assertEquals(id, service.ingest(second));
        assertEquals(before + 1, service.eventCount());
    }

    @Test void resetIsRepeatable() {
        for (int i=0;i<3;i++) {
            service.simulate("expense");
            service.reset();
            assertEquals(22,service.transactions().size());
            assertEquals(22,service.eventCount());
            assertEquals(0,new BigDecimal("100000000").compareTo(db.queryForObject("SELECT balance FROM financial_accounts WHERE id='CHECKING'",BigDecimal.class)));
        }
    }

    @Test void mostRecentlySimulatedTransactionAppearsFirst() {
        String first = service.simulate("high");
        String second = service.simulate("income");

        assertNotEquals(first, second);
        assertEquals(second, service.transactions().getFirst().get("id"));
    }
}
