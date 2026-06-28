package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.geo.GeoService;
import com.iqspark.underwriter.rules.ConfigurableRulesEngine;
import com.iqspark.underwriter.rules.FactExtractor;
import com.iqspark.underwriter.rules.config.RuleConfigLoader;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskProfilingAgentTest {

    private final ConfigurableRulesEngine engine =
            new ConfigurableRulesEngine(new RuleConfigLoader(""), new FactExtractor(new GeoService()));
    private final RiskProfilingAgent agent = new RiskProfilingAgent(engine);

    @Test
    void runsTheRulesEngineAndAddsFindings() {
        assertThat(agent.order()).isEqualTo(20);
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantKnockout());
        agent.handle(ctx);

        assertThat(ctx.findings()).anyMatch(f -> f.code().equals("INSPECTION_INTERVAL_BREACH"));
        assertThat(ctx.findings()).anyMatch(Finding::isKnockout);
        assertThat(ctx.auditTrail().entries()).anyMatch(e -> e.contains("RiskProfilingAgent"));
    }
}
