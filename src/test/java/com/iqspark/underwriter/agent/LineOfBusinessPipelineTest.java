package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.decision.Decision;
import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.geo.GeoService;
import com.iqspark.underwriter.history.AreaRiskService;
import com.iqspark.underwriter.history.HistoricalPolicyRepository;
import com.iqspark.underwriter.history.LogisticRiskModel;
import com.iqspark.underwriter.history.ModelProperties;
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

/** End-to-end decisions across the rental and contents lines through the full agent pipeline. */
class LineOfBusinessPipelineTest {

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
                new PatternLearningAgent(sim, new LogisticRiskModel(repo), new ModelProperties()),
                new ComplianceAgent(),
                new PricingAgent(area));
        orchestrator = new DecisionOrchestrator(agents, template, template, null, null, null, null);
    }

    @Test
    void shortTermRentalWithoutEndorsementDeclines() {
        Decision d = orchestrator.decide(Submissions.rentalStrNoEndorsement());
        assertThat(d.outcome()).isEqualTo(DecisionOutcome.DECLINE);
        assertThat(d.findings()).anyMatch(f -> f.code().equals("STR_WITHOUT_ENDORSEMENT"));
        assertThat(d.indicativePremium()).isNotNull();
    }

    @Test
    void contentsHighValueItemsDoNotAutoApprove() {
        Decision d = orchestrator.decide(Submissions.contents());
        assertThat(d.findings()).anyMatch(f -> f.code().equals("HIGH_VALUE_ITEMS_UNSCHEDULED"));
        assertThat(d.outcome()).isNotEqualTo(DecisionOutcome.APPROVE); // HIGH finding -> at least REFER
    }
}
