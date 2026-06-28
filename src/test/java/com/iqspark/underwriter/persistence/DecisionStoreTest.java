package com.iqspark.underwriter.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iqspark.underwriter.domain.decision.Decision;
import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.Severity;
import com.iqspark.underwriter.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DecisionStoreTest {

    @Autowired
    private DecisionRepository decisions;
    @Autowired
    private AuditEventRepository audits;

    private DecisionStore store;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // mirror Spring Boot's mapper
        store = new DecisionStore(decisions, audits, mapper);
    }

    private Decision decision(String ref) {
        return new Decision(ref, DecisionOutcome.APPROVE, 3,
                List.of(new Finding("OLD_ROOF", Severity.MEDIUM, "RISK", "Roof age 24", "...", "rule")),
                List.of("Obtain roof report"), Money.cad(1000), "rationale",
                null, List.of(), List.of(), null,
                List.of("t | IntakeAgent | x", "t | PricingAgent | y"), Instant.now());
    }

    @Test
    void savesAndReadsBackWithValidAuditChain() {
        store.save(decision("D-1"), "VACANT_HOME", 500_000);

        Optional<StoredDecision> loaded = store.find("D-1");
        assertThat(loaded).isPresent();
        StoredDecision sd = loaded.get();
        assertThat(sd.outcome()).isEqualTo("APPROVE");
        assertThat(sd.findings()).hasSize(1);
        assertThat(sd.conditions()).containsExactly("Obtain roof report");
        assertThat(sd.auditTrail()).hasSize(2);
        assertThat(sd.auditChainValid()).isTrue();
    }

    @Test
    void detectsAuditTampering() {
        store.save(decision("D-2"), "VACANT_HOME", 500_000);
        // Inject a row with a bogus hash/prevHash — the recomputed chain should no longer verify.
        audits.save(new AuditEventEntity("D-2", 99, "tampered", "bogus-prev", "bogus-hash", Instant.now()));

        assertThat(store.verifyAuditChain("D-2")).isFalse();
    }

    @Test
    void missingReferenceIsEmpty() {
        assertThat(store.find("nope")).isEmpty();
    }
}
