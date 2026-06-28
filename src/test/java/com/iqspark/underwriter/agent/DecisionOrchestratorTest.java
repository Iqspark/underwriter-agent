package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.decision.AutonomyTier;
import com.iqspark.underwriter.domain.decision.Decision;
import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.autonomy.AutonomyProperties;
import com.iqspark.underwriter.autonomy.AutonomyRouter;
import com.iqspark.underwriter.geo.GeoService;
import com.iqspark.underwriter.history.AreaRiskService;
import com.iqspark.underwriter.history.HistoricalPolicyRepository;
import com.iqspark.underwriter.history.SimilarityEngine;
import com.iqspark.underwriter.llm.TemplateLlmReasoner;
import com.iqspark.underwriter.review.ReviewerAgent;
import com.iqspark.underwriter.rules.ConfigurableRulesEngine;
import com.iqspark.underwriter.rules.FactExtractor;
import com.iqspark.underwriter.rules.config.RuleConfigLoader;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionOrchestratorTest {

    private DecisionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        GeoService geo = new GeoService();
        ConfigurableRulesEngine rules =
                new ConfigurableRulesEngine(new RuleConfigLoader(""), new FactExtractor(geo));
        HistoricalPolicyRepository repo = new HistoricalPolicyRepository(1500, 42);
        AreaRiskService area = new AreaRiskService(repo);
        SimilarityEngine sim = new SimilarityEngine(repo, area, 25);
        TemplateLlmReasoner template = new TemplateLlmReasoner();

        List<UnderwritingAgent> agents = List.of(
                new IntakeAgent(),
                new RiskProfilingAgent(rules),
                new PatternLearningAgent(sim),
                new ComplianceAgent(),
                new PricingAgent(area));

        orchestrator = new DecisionOrchestrator(agents, template, template, null, null,
                new ReviewerAgent(null), new AutonomyRouter(new AutonomyProperties()));
    }

    @Test
    void knockoutDeclines() {
        Decision d = orchestrator.decide(Submissions.vacantKnockout());
        assertThat(d.outcome()).isEqualTo(DecisionOutcome.DECLINE);
        assertThat(d.findings()).anyMatch(Finding::isKnockout);
        assertThat(d.conditions()).isNotEmpty();
        assertThat(d.indicativePremium()).isNotNull();
        assertThat(d.auditTrail()).isNotEmpty();
        assertThat(d.autonomy().tier()).isEqualTo(AutonomyTier.SPECIALIST); // knockout -> senior
    }

    @Test
    void cleanFileDoesNotDecline() {
        Decision d = orchestrator.decide(Submissions.vacantClean());
        assertThat(d.outcome()).isNotEqualTo(DecisionOutcome.DECLINE);
        assertThat(d.rationale()).isNotBlank();
        assertThat(d.learnedAssessment()).isNotNull();
    }

    @Test
    void highRiskFileDoesNotApprove() {
        Decision d = orchestrator.decide(Submissions.vacantRemoteRisky());
        assertThat(d.outcome()).isNotEqualTo(DecisionOutcome.APPROVE);
    }

    @Test
    void riskScoreIsSumOfFindingWeights() {
        Decision d = orchestrator.decide(Submissions.vacantRemoteRisky());
        int expected = d.findings().stream().mapToInt(Finding::weight).sum();
        assertThat(d.riskScore()).isEqualTo(expected);
    }
}
