package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.history.AreaRiskService;
import com.iqspark.underwriter.history.HistoricalPolicyRepository;
import com.iqspark.underwriter.history.model.AreaRiskStat;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import com.iqspark.underwriter.history.model.Peril;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingAgentTest {

    private final HistoricalPolicyRepository repo = new HistoricalPolicyRepository(500, 42);
    private final PricingAgent agent = new PricingAgent(new AreaRiskService(repo));

    @Test
    void runsLastAndPricesOnColdStartBaseRate() {
        assertThat(agent.order()).isEqualTo(40);
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantClean()); // no learned set
        agent.handle(ctx);
        assertThat(ctx.indicativePremium()).isNotNull();
        assertThat(ctx.indicativePremium().currency()).isEqualTo("CAD");
        assertThat(ctx.indicativePremium().amount()).isGreaterThanOrEqualTo(750.0); // floor
    }

    @Test
    void pricesFromComparableRateWhenLearned() {
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantClean()); // coverage 900k
        ctx.setLearnedAssessment(new LearnedAssessment(20, 0.85, 0.2, 0.6, 7.0, Peril.NONE,
                "HIGH", false, List.of(), AreaRiskStat.unknown("Toronto")));
        agent.handle(ctx);
        // ~ (900000/1000) * 7.0 * areaLoad, floored — comfortably above the floor.
        assertThat(ctx.indicativePremium().amount()).isGreaterThan(1000.0);
    }
}
