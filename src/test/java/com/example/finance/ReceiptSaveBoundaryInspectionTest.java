package com.example.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Read-only design inspection of the existing save contract, not TC07 acceptance. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:receipt_boundary_inspection;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","finbridge.llm.enabled=false"})
class ReceiptSaveBoundaryInspectionTest {
    @Autowired DemoDataService demo;
    @Autowired TransactionService transactions;
    @Autowired JdbcTemplate db;
    @BeforeEach void reset(){demo.resetAll();}
    String save(LocalDate date,String reference){
        return transactions.recordManualTransaction("TC07 inspection coffee",new BigDecimal("50000"),"VND",date,"cash","Food & Drinks",reference);
    }
    @Test void differentReferencesStillCollapseSameMerchantAmountAndDate(){
        int before=db.queryForObject("SELECT COUNT(*) FROM transactions",Integer.class);
        var date=LocalDate.now();String first=save(date,"TC07-PURCHASE-ONE");String second=save(date,"TC07-PURCHASE-TWO");
        assertEquals(first,second,"Existing normal ingestion returns the first transaction despite different purchase references");
        assertEquals(before+1,db.queryForObject("SELECT COUNT(*) FROM transactions",Integer.class));
        assertEquals(date.atStartOfDay(),db.queryForObject("SELECT occurred_at FROM transactions WHERE id=?",java.time.LocalDateTime.class,first));
    }
    @Test void monthlyPurchaseOutsideWindowCanUseTheUnchangedNormalPath(){
        int before=db.queryForObject("SELECT COUNT(*) FROM transactions",Integer.class);
        var date=LocalDate.now();String first=save(date,"TC07-MONTH-ONE");String second=save(date.plusDays(30),"TC07-MONTH-TWO");
        assertNotEquals(first,second);assertEquals(before+2,db.queryForObject("SELECT COUNT(*) FROM transactions",Integer.class));
    }
}