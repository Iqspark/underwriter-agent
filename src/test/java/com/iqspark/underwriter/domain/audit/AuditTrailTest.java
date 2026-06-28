package com.iqspark.underwriter.domain.audit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditTrailTest {

    @Test
    void recordsOrderedEntriesWithAgentAndDetail() {
        AuditTrail trail = new AuditTrail();
        trail.record("IntakeAgent", "ingested");
        trail.record("PricingAgent", "priced");

        List<String> entries = trail.entries();
        assertThat(trail.size()).isEqualTo(2);
        assertThat(entries.get(0)).contains("IntakeAgent").contains("ingested").contains(" | ");
        assertThat(entries.get(1)).contains("PricingAgent").contains("priced");
    }

    @Test
    void entriesAreAnImmutableCopy() {
        AuditTrail trail = new AuditTrail();
        trail.record("A", "x");
        List<String> snapshot = trail.entries();
        assertThatThrownBy(() -> snapshot.add("tamper"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
