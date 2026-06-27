package com.iqspark.underwriter.history;

import com.iqspark.underwriter.history.model.AreaRiskStat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AreaRiskServiceTest {

    private final HistoricalPolicyRepository repo = new HistoricalPolicyRepository(1000, 42);
    private final AreaRiskService service = new AreaRiskService(repo);

    @Test
    void knownAreaHasStatsInRange() {
        AreaRiskStat stat = service.forCity("Toronto");
        assertThat(stat.sampleSize()).isGreaterThan(0);
        assertThat(stat.overallClaimRate()).isBetween(0.0, 1.0);
        assertThat(stat.theftClaimRate()).isBetween(0.0, 1.0);
    }

    @Test
    void unknownAreaIsNeutral() {
        AreaRiskStat stat = service.forCity("Atlantis");
        assertThat(stat.sampleSize()).isZero();
        assertThat(service.theftLoad("Atlantis")).isEqualTo(1.0);
    }

    @Test
    void theftLoadIsBounded() {
        assertThat(service.theftLoad("Toronto")).isBetween(0.9, 1.5);
        assertThat(service.theftLoad("Flin Flon")).isBetween(0.9, 1.5);
    }
}
