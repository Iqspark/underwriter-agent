package com.iqspark.underwriter.drafting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DraftingControllerIntegrationTest {

    private static final String SUBMISSION = """
            {"reference":"DRAFT-1","lineOfBusiness":"VACANT_HOME",
             "applicant":{"name":"A","priorLossesDeclared":false,"priorLossCount":0},
             "location":{"city":"Toronto","province":"ON","latitude":43.6532,"longitude":-79.3832},
             "building":{"construction":"Masonry","occupancyType":"Detached Home","units":1,
                         "squareFeet":2400,"yearBuilt":2012,"roofAgeYears":6,
                         "renovationPlanned":false,"demolitionPlanned":false},
             "vacancy":{"vacantSince":"2026-01-01","inspectionIntervalHours":24,
                        "utilitiesOn":true,"waterShutOff":true,"securitySystem":true},
             "protection":{"monitoredAlarm":true,"sprinklered":true,
                           "distanceToHydrantMeters":40,"distanceToFireHallKm":3},
             "requestedCoverage":{"amount":500000,"currency":"CAD"}}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    @WithMockUser(roles = "UNDERWRITER")
    void draftsAreAvailableForAStoredDecision() throws Exception {
        mvc.perform(post("/api/underwriting/submissions")
                        .contentType(MediaType.APPLICATION_JSON).content(SUBMISSION))
                .andExpect(status().isOk());

        mvc.perform(get("/api/underwriting/decisions/DRAFT-1/drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brokerEmail").exists())
                .andExpect(jsonPath("$.underwriterMemo").exists());
    }
}
