package com.iqspark.underwriter.metrics;

import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.model.LineOfBusiness;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Business metrics for the dashboards (doc 10 §4): decision mix by outcome/line/tier, premium
 * distribution, and realized-outcome counts — all exported via Micrometer/Prometheus.
 */
@Component
public class DecisionMetrics {

    private final MeterRegistry registry;

    public DecisionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordDecision(DecisionOutcome outcome, LineOfBusiness line, String tier, double premium) {
        registry.counter("underwriting.decisions",
                "outcome", outcome.name(),
                "line", line.name(),
                "tier", tier == null ? "NONE" : tier).increment();
        registry.summary("underwriting.premium", "line", line.name()).record(premium);
    }

    public void recordOutcome(boolean hadClaim) {
        registry.counter("underwriting.outcomes", "claim", String.valueOf(hadClaim)).increment();
    }
}
