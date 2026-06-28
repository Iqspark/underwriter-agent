package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.history.AreaRiskService;
import com.iqspark.underwriter.history.HistoricalPolicyRepository;
import com.iqspark.underwriter.history.LogisticRiskModel;
import com.iqspark.underwriter.history.ModelProperties;
import com.iqspark.underwriter.history.SimilarityEngine;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PatternLearningAgentTest {

    @Test
    void learnsAndBlendsModelWithKnn() {
        HistoricalPolicyRepository repo = new HistoricalPolicyRepository(1500, 42);
        AreaRiskService area = new AreaRiskService(repo);
        PatternLearningAgent agent = new PatternLearningAgent(
                new SimilarityEngine(repo, area, 25), new LogisticRiskModel(repo), new ModelProperties());

        assertThat(agent.order()).isEqualTo(25);
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantClean());
        agent.handle(ctx);

        assertThat(ctx.learnedAssessment()).isNotNull();
        assertThat(ctx.learnedAssessment().coldStart()).isFalse();
        assertThat(ctx.learnedAssessment().claimProbability()).isBetween(0.0, 1.0);
        assertThat(ctx.findings()).anyMatch(f -> f.code().equals("LEARNED_CLAIM_PROBABILITY"));
        assertThat(ctx.auditTrail().entries()).anyMatch(e -> e.contains("Hybrid claimProb"));
    }

    @Test
    void coldStartWhenBookIsTiny() {
        HistoricalPolicyRepository tiny = new HistoricalPolicyRepository(3, 42);
        AreaRiskService area = new AreaRiskService(tiny);
        PatternLearningAgent agent = new PatternLearningAgent(
                new SimilarityEngine(tiny, area, 25), new LogisticRiskModel(tiny), new ModelProperties());

        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantClean());
        agent.handle(ctx);

        assertThat(ctx.learnedAssessment().coldStart()).isTrue();
        assertThat(ctx.findings()).noneMatch(f -> f.code().equals("LEARNED_CLAIM_PROBABILITY"));
        assertThat(ctx.auditTrail().entries()).anyMatch(e -> e.contains("Cold start"));
    }
}
