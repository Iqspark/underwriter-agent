package com.iqspark.underwriter.history;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureRangesTest {

    private final FeatureRanges ranges = new HistoricalPolicyRepository(300, 42).featureRanges();

    @Test
    void normalizesIntoUnitInterval() {
        assertThat(ranges.normalize("roofAgeYears", 20)).isBetween(0.0, 1.0);
    }

    @Test
    void clampsBelowMinAndAboveMax() {
        assertThat(ranges.normalize("roofAgeYears", -1_000_000)).isEqualTo(0.0);
        assertThat(ranges.normalize("roofAgeYears", 1_000_000)).isEqualTo(1.0);
    }

    @Test
    void unknownFeatureNormalizesToZero() {
        assertThat(ranges.normalize("does-not-exist", 42)).isEqualTo(0.0);
    }
}
