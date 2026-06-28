package com.iqspark.underwriter.llm;

import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.RetrievedSource;
import com.iqspark.underwriter.domain.decision.Severity;
import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateLlmReasonerTest {

    private final TemplateLlmReasoner reasoner = new TemplateLlmReasoner();

    @Test
    void leadsWithTheRecommendedActionAndKnockout() {
        List<Finding> findings = List.of(new Finding("INSPECTION_INTERVAL_BREACH", Severity.KNOCKOUT,
                "COMPLIANCE", "Inspection interval 168h exceeds the 72h condition precedent", "cure", "rule"));
        String out = reasoner.summarize(Submissions.vacantKnockout(), DecisionOutcome.DECLINE,
                findings, null, Money.cad(1000), List.of());

        assertThat(out).startsWith("Recommended action: DECLINE");
        assertThat(out.toLowerCase()).contains("knockout");
        assertThat(out).contains("168h");
    }

    @Test
    void notesColdStartWhenNoLearnedAssessment() {
        String out = reasoner.summarize(Submissions.vacantClean(), DecisionOutcome.APPROVE,
                List.of(), null, Money.cad(1000), List.of());
        assertThat(out.toLowerCase()).contains("cold start");
        assertThat(out).contains("approve on standard terms");
    }

    @Test
    void appendsGroundedSourceIds() {
        String out = reasoner.summarize(Submissions.vacantClean(), DecisionOutcome.REFER,
                List.of(), null, Money.cad(1200),
                List.of(new RetrievedSource("PR0003-cl2", "WORDING", 0.9, "snippet")));
        assertThat(out).contains("PR0003-cl2");
    }

    @Test
    void nameIsTemplateOffline() {
        assertThat(reasoner.name()).isEqualTo("template-offline");
    }
}
