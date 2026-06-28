package com.iqspark.underwriter.domain.decision;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FindingTest {

    @Test
    void weightDelegatesToSeverity() {
        Finding f = new Finding("OLD_ROOF", Severity.MEDIUM, "RISK", "Roof age 30", "...", "rule");
        assertThat(f.weight()).isEqualTo(Severity.MEDIUM.weight());
        assertThat(f.isKnockout()).isFalse();
    }

    @Test
    void knockoutIsFlagged() {
        Finding ko = new Finding("INSPECTION_INTERVAL_BREACH", Severity.KNOCKOUT, "COMPLIANCE",
                "168h", "...", "rule");
        assertThat(ko.isKnockout()).isTrue();
        assertThat(ko.weight()).isEqualTo(100);
    }
}
