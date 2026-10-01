package com.example.finance;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:production_ui;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.demo-tools-enabled=false"
})
@AutoConfigureMockMvc
class ProductionUiIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void productionModeShowsHistoryWithoutDemoTransactionTools() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("All transactions")))
                .andExpect(content().string(containsString("Needs your review")))
                .andExpect(content().string(containsString("Your transaction categories")))
                .andExpect(content().string(containsString("Recent transactions")))
                .andExpect(content().string(not(containsString("Demo bank feed"))))
                .andExpect(content().string(not(containsString("data-testid=\"payment-demo-tools\""))))
                .andExpect(content().string(not(containsString("data-testid=\"simulate-high\""))));
    }
}
