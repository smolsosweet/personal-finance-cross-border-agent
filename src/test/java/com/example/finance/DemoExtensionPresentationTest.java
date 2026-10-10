package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties={"finbridge.demo.presentation=extension","spring.datasource.url=jdbc:h2:mem:extension_presentation;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","finbridge.llm.enabled=false"})
@AutoConfigureMockMvc
class DemoExtensionPresentationTest {
    @Autowired MockMvc mvc;
    @Autowired DemoPresentationScope scope;
    @Test void extensionChannelsAndCorridorsRemainAvailable() throws Exception {
        assertFalse(scope.competition());
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-testid=\"channel-MOMO\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-testid=\"channel-VCB\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-testid=\"channel-TCB\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"United States\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"Australia\"")));
    }
}