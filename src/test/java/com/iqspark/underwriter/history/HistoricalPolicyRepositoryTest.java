package com.iqspark.underwriter.history;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricalPolicyRepositoryTest {

    @Test
    void generatesBookOfRequestedSizeWithSaneStats() {
        HistoricalPolicyRepository repo = new HistoricalPolicyRepository(500, 42);
        assertThat(repo.size()).isEqualTo(500);
        assertThat(repo.all()).hasSize(500);
        assertThat(repo.overallClaimRate()).isBetween(0.0, 1.0);
        assertThat(repo.featureRanges()).isNotNull();

        HistoricalPolicyRepository.BookStats stats = repo.stats();
        assertThat(stats.size()).isEqualTo(500);
        assertThat(stats.claimRate()).isEqualTo(repo.overallClaimRate());
    }

    @Test
    void sameSeedProducesTheSameBook() {
        HistoricalPolicyRepository a = new HistoricalPolicyRepository(200, 42);
        HistoricalPolicyRepository b = new HistoricalPolicyRepository(200, 42);
        assertThat(a.all().get(0)).isEqualTo(b.all().get(0));
        assertThat(a.overallClaimRate()).isEqualTo(b.overallClaimRate());
    }
}
