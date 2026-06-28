package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.enrichment.EnrichmentProperties;
import com.iqspark.underwriter.enrichment.EnrichmentService;
import com.iqspark.underwriter.enrichment.OfflineEnrichmentProvider;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnrichmentAgentTest {

    private EnrichmentAgent agent(EnrichmentProperties props) {
        return new EnrichmentAgent(new EnrichmentService(new OfflineEnrichmentProvider(), props), props);
    }

    @Test
    void runsAtOrder15() {
        assertThat(agent(new EnrichmentProperties()).order()).isEqualTo(15);
    }

    @Test
    void elevatedPerilRaisesFindingsAndSetsEnrichment() {
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantRemoteRisky()); // Flin Flon, MB
        agent(new EnrichmentProperties()).handle(ctx);

        assertThat(ctx.enrichment()).isNotNull();
        assertThat(ctx.enrichment().available()).isTrue();
        assertThat(ctx.findings()).anyMatch(f -> f.code().equals("ENRICHED_CRIME_ELEVATED"));
        // Advisory enrichment never produces a knockout.
        assertThat(ctx.findings()).noneMatch(f -> f.isKnockout());
    }

    @Test
    void unavailableEnrichmentAddsNoFindings() {
        UnderwritingContext ctx = new UnderwritingContext(Submissions.missingMost()); // no location
        agent(new EnrichmentProperties()).handle(ctx);

        assertThat(ctx.enrichment().available()).isFalse();
        assertThat(ctx.findings()).isEmpty();
    }

    @Test
    void disabledAgentDoesNothing() {
        EnrichmentProperties props = new EnrichmentProperties();
        props.setEnabled(false);
        UnderwritingContext ctx = new UnderwritingContext(Submissions.vacantRemoteRisky());
        agent(props).handle(ctx);

        assertThat(ctx.enrichment()).isNull();
        assertThat(ctx.findings()).isEmpty();
    }
}
