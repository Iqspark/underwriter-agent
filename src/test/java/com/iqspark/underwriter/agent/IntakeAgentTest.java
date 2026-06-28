package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntakeAgentTest {

    private final IntakeAgent agent = new IntakeAgent();

    @Test
    void runsFirstAndLogsTheSubmission() {
        assertThat(agent.order()).isEqualTo(10);
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantClean());
        agent.handle(ctx);
        assertThat(ctx.auditTrail().entries())
                .anyMatch(e -> e.contains("IntakeAgent") && e.contains("Toronto"));
    }
}
