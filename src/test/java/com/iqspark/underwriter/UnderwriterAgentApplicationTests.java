package com.iqspark.underwriter;

import com.iqspark.underwriter.agent.DecisionOrchestrator;
import com.iqspark.underwriter.domain.decision.Decision;
import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Full Spring context smoke test: the application wires up and decides offline end-to-end. */
@SpringBootTest
class UnderwriterAgentApplicationTests {

    @Autowired
    private DecisionOrchestrator orchestrator;

    @Test
    void contextLoads() {
        assertThat(orchestrator).isNotNull();
    }

    @Test
    void decidesAndPersistsAKnockoutEndToEnd() {
        Decision d = orchestrator.decide(Submissions.vacantKnockout());
        assertThat(d.outcome()).isEqualTo(DecisionOutcome.DECLINE);
        assertThat(d.auditTrail()).isNotEmpty();
    }
}
