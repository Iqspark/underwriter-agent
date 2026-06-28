package com.iqspark.underwriter.enrichment;

import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OfflineEnrichmentProviderTest {

    private final OfflineEnrichmentProvider provider = new OfflineEnrichmentProvider();

    @Test
    void enrichesALocatedSubmissionDeterministically() {
        Enrichment a = provider.enrich(Submissions.vacantRemoteRisky()); // Flin Flon, MB
        Enrichment b = provider.enrich(Submissions.vacantRemoteRisky());
        assertThat(a.available()).isTrue();
        assertThat(a).isEqualTo(b); // deterministic
        assertThat(a.crimeScore()).isBetween(0.0, 1.0);
        assertThat(a.crimeScore()).isGreaterThan(0.6); // Flin Flon is high-crime in the table
    }

    @Test
    void unavailableWhenNoLocation() {
        assertThat(provider.enrich(Submissions.missingMost()).available()).isFalse();
    }
}
