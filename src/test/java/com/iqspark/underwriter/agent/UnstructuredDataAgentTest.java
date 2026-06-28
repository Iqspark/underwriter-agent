package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.intake.SemanticExtractionProperties;
import com.iqspark.underwriter.intake.SemanticFeatureExtractor;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnstructuredDataAgentTest {

    private UnstructuredDataAgent agent(SemanticExtractionProperties props) {
        return new UnstructuredDataAgent(new SemanticFeatureExtractor(null), props);
    }

    @Test
    void runsAtOrder12() {
        assertThat(agent(new SemanticExtractionProperties()).order()).isEqualTo(12);
    }

    @Test
    void extractsFeaturesFromNotesAndRaisesAdvisoryFindings() {
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantRemoteRisky()); // has notes
        agent(new SemanticExtractionProperties()).handle(ctx);

        assertThat(ctx.semanticFeatures()).isNotNull();
        assertThat(ctx.semanticFeatures().present()).isTrue();
        assertThat(ctx.findings()).anyMatch(f -> f.code().equals("SEMANTIC_DEFERRED_MAINTENANCE"));
        assertThat(ctx.findings()).anyMatch(f -> f.code().equals("SEMANTIC_HAZARDS"));
        assertThat(ctx.findings()).noneMatch(Finding::isKnockout); // advisory only
    }

    @Test
    void noNotesYieldsNoFindings() {
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantClean()); // notes == null
        agent(new SemanticExtractionProperties()).handle(ctx);
        assertThat(ctx.semanticFeatures().present()).isFalse();
        assertThat(ctx.findings()).isEmpty();
    }

    @Test
    void disabledDoesNothing() {
        SemanticExtractionProperties props = new SemanticExtractionProperties();
        props.setSemanticFeaturesEnabled(false);
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantRemoteRisky());
        agent(props).handle(ctx);
        assertThat(ctx.semanticFeatures()).isNull();
        assertThat(ctx.findings()).isEmpty();
    }
}
