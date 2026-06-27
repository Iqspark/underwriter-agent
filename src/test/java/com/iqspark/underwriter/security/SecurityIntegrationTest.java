package com.iqspark.underwriter.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    private static final String CLEAN_SUBMISSION = """
            {"reference":"SEC-1","lineOfBusiness":"VACANT_HOME",
             "applicant":{"name":"A","priorLossesDeclared":false,"priorLossCount":0},
             "location":{"city":"Toronto","province":"ON","latitude":43.6532,"longitude":-79.3832},
             "building":{"construction":"Masonry","occupancyType":"Detached Home","units":1,
                         "squareFeet":2400,"yearBuilt":2012,"roofAgeYears":6,
                         "renovationPlanned":false,"demolitionPlanned":false},
             "vacancy":{"vacantSince":"2026-01-01","inspectionIntervalHours":24,
                        "utilitiesOn":true,"waterShutOff":true,"securitySystem":true},
             "protection":{"monitoredAlarm":true,"sprinklered":true,
                           "distanceToHydrantMeters":40,"distanceToFireHallKm":3},
             "requestedCoverage":{"amount":900000,"currency":"CAD"}}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/api/underwriting/health")).andExpect(status().isOk());
    }

    @Test
    void underwritingRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/underwriting/submissions")
                        .contentType(MediaType.APPLICATION_JSON).content(CLEAN_SUBMISSION))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void auditorCannotUnderwrite() throws Exception {
        mvc.perform(post("/api/underwriting/submissions")
                        .contentType(MediaType.APPLICATION_JSON).content(CLEAN_SUBMISSION))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "UNDERWRITER")
    void underwriterCanUnderwrite() throws Exception {
        mvc.perform(post("/api/underwriting/submissions")
                        .contentType(MediaType.APPLICATION_JSON).content(CLEAN_SUBMISSION))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorMetricsAreNotPublic() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanReadActuator() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().isOk());
    }
}
