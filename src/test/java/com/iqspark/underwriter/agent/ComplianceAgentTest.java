package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.Severity;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceAgentTest {

    private final ComplianceAgent agent = new ComplianceAgent();

    @Test
    void recordsClearanceWhenNoKnockout() {
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantClean());
        agent.handle(ctx);
        assertThat(ctx.auditTrail().entries()).anyMatch(e -> e.contains("clearance"));
    }

    @Test
    void recordsBreachWhenAKnockoutIsPresent() {
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantClean());
        ctx.addFinding(new Finding("INSPECTION_INTERVAL_BREACH", Severity.KNOCKOUT, "COMPLIANCE",
                "168h", "cure", "rule"));
        agent.handle(ctx);
        assertThat(ctx.auditTrail().entries())
                .anyMatch(e -> e.contains("breach") && e.contains("INSPECTION_INTERVAL_BREACH"));
    }
}
