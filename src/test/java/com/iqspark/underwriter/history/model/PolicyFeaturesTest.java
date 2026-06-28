package com.iqspark.underwriter.history.model;

import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyFeaturesTest {

    @Test
    void extractsNumericAndCategoricalFromASubmission() {
        PolicyFeatures f = PolicyFeatures.fromSubmission(Submissions.vacantClean());
        assertThat(f.num("roofAgeYears")).isEqualTo(6.0);
        assertThat(f.num("coverageAmount")).isEqualTo(900_000.0);
        assertThat(f.num("securitySystem")).isEqualTo(1.0);   // boolean true -> 1
        assertThat(f.num("monitoredAlarm")).isEqualTo(1.0);
        assertThat(f.cat("city")).isEqualTo("Toronto");
        assertThat(f.cat("province")).isEqualTo("ON");
        assertThat(f.num("vacancyMonths")).isGreaterThanOrEqualTo(1.0); // vacant ~2 months
    }

    @Test
    void missingSectionsDefaultToZeroAndEmpty() {
        PolicyFeatures f = PolicyFeatures.fromSubmission(Submissions.contents()); // no building/vacancy
        assertThat(f.num("roofAgeYears")).isEqualTo(0.0);
        assertThat(f.num("vacancyMonths")).isEqualTo(0.0);
        assertThat(f.cat("construction")).isEmpty();
    }

    @Test
    void accessorsDefaultForAbsentKeys() {
        PolicyFeatures empty = new PolicyFeatures(Map.of(), Map.of());
        assertThat(empty.num("anything")).isEqualTo(0.0);
        assertThat(empty.cat("anything")).isEmpty();
        assertThat(PolicyFeatures.NUMERIC_KEYS).isNotEmpty();
        assertThat(PolicyFeatures.CATEGORICAL_KEYS).contains("city", "province");
    }
}
