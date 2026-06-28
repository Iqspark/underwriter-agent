package com.iqspark.underwriter.domain.decision;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeverityTest {

    @Test
    void weightsMatchTheAppetiteScale() {
        assertThat(Severity.INFO.weight()).isZero();
        assertThat(Severity.LOW.weight()).isEqualTo(1);
        assertThat(Severity.MEDIUM.weight()).isEqualTo(3);
        assertThat(Severity.HIGH.weight()).isEqualTo(6);
        assertThat(Severity.KNOCKOUT.weight()).isEqualTo(100);
    }

    @Test
    void severityIncreasesWithWeight() {
        assertThat(Severity.KNOCKOUT.weight()).isGreaterThan(Severity.HIGH.weight());
        assertThat(Severity.HIGH.weight()).isGreaterThan(Severity.MEDIUM.weight());
        assertThat(Severity.MEDIUM.weight()).isGreaterThan(Severity.LOW.weight());
        assertThat(Severity.LOW.weight()).isGreaterThan(Severity.INFO.weight());
    }
}
