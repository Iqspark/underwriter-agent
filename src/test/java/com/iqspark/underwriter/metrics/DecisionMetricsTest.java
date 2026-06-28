package com.iqspark.underwriter.metrics;

import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.model.LineOfBusiness;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final DecisionMetrics metrics = new DecisionMetrics(registry);

    @Test
    void recordsDecisionCounterAndPremiumSummary() {
        metrics.recordDecision(DecisionOutcome.APPROVE, LineOfBusiness.VACANT_HOME, "AUTO", 1234.0);

        assertThat(registry.get("underwriting.decisions")
                .tag("outcome", "APPROVE").tag("tier", "AUTO").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("underwriting.premium").summary().count()).isEqualTo(1);
    }

    @Test
    void recordsOutcomeCounter() {
        metrics.recordOutcome(true);
        assertThat(registry.get("underwriting.outcomes").tag("claim", "true").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void nullTierIsRecordedAsNone() {
        metrics.recordDecision(DecisionOutcome.REFER, LineOfBusiness.RENTAL, null, 0.0);
        assertThat(registry.get("underwriting.decisions").tag("tier", "NONE").counter().count())
                .isEqualTo(1.0);
    }
}
