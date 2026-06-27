package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.decision.Decision;
import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.geo.GeoService;
import com.iqspark.underwriter.history.AreaRiskService;
import com.iqspark.underwriter.history.HistoricalPolicyRepository;
import com.iqspark.underwriter.history.SimilarityEngine;
import com.iqspark.underwriter.llm.TemplateLlmReasoner;
import com.iqspark.underwriter.rules.ConfigurableRulesEngine;
import com.iqspark.underwriter.rules.FactExtractor;
import com.iqspark.underwriter.rules.config.RuleConfigLoader;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end (no Spring) checks of the AI-first pipeline: learning attaches, agents run in order. */
class AiFirstPipelineTest {

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
        orchestrator = new DecisionOrchestrator(agents, template, template, null, null);
    }

    @Test
    void learnedAssessmentDrivesAndIsAttached() {
        Decision d = orchestrator.decide(Submissions.vacantClean());
        assertThat(d.learnedAssessment()).isNotNull();
        assertThat(d.learnedAssessment().coldStart()).isFalse();
        assertThat(d.learnedAssessment().topComparables()).isNotEmpty();
    }

    @Test
    void everyAgentContributesToTheAuditTrail() {
        Decision d = orchestrator.decide(Submissions.vacantClean());
        assertThat(d.auditTrail()).anyMatch(s -> s.contains("IntakeAgent"));
        assertThat(d.auditTrail()).anyMatch(s -> s.contains("RiskProfilingAgent"));
        assertThat(d.auditTrail()).anyMatch(s -> s.contains("PatternLearningAgent"));
        assertThat(d.auditTrail()).anyMatch(s -> s.contains("ComplianceAgent"));
        assertThat(d.auditTrail()).anyMatch(s -> s.contains("PricingAgent"));
        assertThat(d.auditTrail()).anyMatch(s -> s.contains("LlmReasoner"));
        assertThat(d.auditTrail()).anyMatch(s -> s.contains("DecisionOrchestrator"));
    }

    @Test
    void rationaleLeadsWithTheRecommendedAction() {
        Decision d = orchestrator.decide(Submissions.vacantClean());
        assertThat(d.rationale()).startsWith("Recommended action:");
    }

    @Test
    void knockoutAttachesACuringCondition() {
        Decision d = orchestrator.decide(Submissions.vacantKnockout());
        assertThat(d.outcome()).isEqualTo(DecisionOutcome.DECLINE);
        assertThat(d.conditions()).anyMatch(c -> c.toLowerCase().contains("72-hour"));
    }
}
