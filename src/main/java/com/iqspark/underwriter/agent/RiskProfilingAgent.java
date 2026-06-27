package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.rules.ConfigurableRulesEngine;
import org.springframework.stereotype.Component;

import java.util.List;

/** Runs the deterministic rules engine (guardrails) over the submission. */
@Component
public class RiskProfilingAgent implements UnderwritingAgent {

    private final ConfigurableRulesEngine rulesEngine;

    public RiskProfilingAgent(ConfigurableRulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void handle(UnderwritingContext context) {
        List<Finding> findings = rulesEngine.evaluate(context.submission());
        context.addFindings(findings);
        int weight = findings.stream().mapToInt(Finding::weight).sum();
        context.audit("RiskProfilingAgent",
                "Evaluated guardrail rules, raised %d findings (risk weight=%d)"
                        .formatted(findings.size(), weight));
    }
}
