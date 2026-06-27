package com.iqspark.underwriter.metrics;

import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.model.LineOfBusiness;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Baseline decision metrics: a counter tagged by outcome and line of business. */
@Component
public class DecisionMetrics {

    private final MeterRegistry registry;

    public DecisionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(DecisionOutcome outcome, LineOfBusiness line) {
        registry.counter("underwriting.decisions",
                "outcome", outcome.name(),
                "line", line.name()).increment();
    }
}
