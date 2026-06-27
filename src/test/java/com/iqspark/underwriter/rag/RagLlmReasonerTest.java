package com.iqspark.underwriter.rag;

import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.decision.RetrievedSource;
import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.llm.TemplateLlmReasoner;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagLlmReasonerTest {

    private final RagLlmReasoner reasoner = new RagLlmReasoner(null, new TemplateLlmReasoner());

    @Test
    void offlineRationaleCitesSourceIds() {
        String rationale = reasoner.summarize(
                Submissions.vacantClean(), DecisionOutcome.REFER, List.of(), null, Money.cad(1200),
                List.of(new RetrievedSource("PR0003-cl2", "WORDING", 0.9, "inspect every 72 hours")));
        assertThat(rationale).contains("PR0003-cl2");
        assertThat(rationale).startsWith("Recommended action:");
    }

    @Test
    void nameIsRagGrounded() {
        assertThat(reasoner.name()).isEqualTo("rag-grounded");
    }
}
