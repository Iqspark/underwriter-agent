package com.iqspark.underwriter.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @WithMockUser(roles = "UNDERWRITER")
    void exposesDashboardKpis() throws Exception {
        mvc.perform(get("/api/underwriting/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDecisions").exists())
                .andExpect(jsonPath("$.stpRate").exists());
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/underwriting/dashboard")).andExpect(status().isUnauthorized());
    }
}
