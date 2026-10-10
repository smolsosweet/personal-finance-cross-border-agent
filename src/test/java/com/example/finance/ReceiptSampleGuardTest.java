package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tc07_demo_disabled;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.demo-tools-enabled=false","finbridge.llm.enabled=false"})
@AutoConfigureMockMvc
class ReceiptSampleGuardTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Test void sampleRouteAndButtonAreUnavailableWhenDemoToolsDisabled() throws Exception {
        int before=db.queryForObject("SELECT COUNT(*) FROM receipt_submissions",Integer.class);
        mvc.perform(post("/transactions/receipt/sample")).andExpect(status().isNotFound());
        mvc.perform(get("/")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("data-receipt-sample"))));
        assertEquals(before,db.queryForObject("SELECT COUNT(*) FROM receipt_submissions",Integer.class));
    }
}