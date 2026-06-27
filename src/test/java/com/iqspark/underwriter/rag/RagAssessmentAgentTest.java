package com.iqspark.underwriter.rag;

import com.iqspark.underwriter.agent.UnderwritingContext;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.RetrievedSource;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagAssessmentAgentTest {

    private final UnderwritingRetriever retriever = mock(UnderwritingRetriever.class);
    private final RagAssessmentAgent agent = new RagAssessmentAgent(retriever, null, new RagProperties());

    @Test
    void runsAtOrder26() {
        assertThat(agent.order()).isEqualTo(26);
    }

    @Test
    void attachesGroundingAndSourcesOffline() {
        when(retriever.retrieve(any())).thenReturn(List.of(
                new RetrievedSource("PR0003-cl2", "WORDING", 0.9, "inspect every 72 hours")));
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantClean());

        agent.handle(ctx);

        assertThat(ctx.retrievedSources()).isNotEmpty();
        assertThat(ctx.findings()).anyMatch(f -> f.code().equals("RAG_GROUNDING"));
        // Advisory only — never a knockout.
        assertThat(ctx.findings()).noneMatch(Finding::isKnockout);
    }

    @Test
    void noSourcesNoFinding() {
        when(retriever.retrieve(any())).thenReturn(List.of());
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantClean());

        agent.handle(ctx);

        assertThat(ctx.retrievedSources()).isEmpty();
        assertThat(ctx.findings()).isEmpty();
    }
}
