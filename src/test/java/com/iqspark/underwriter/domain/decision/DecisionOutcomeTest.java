package com.iqspark.underwriter.domain.decision;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionOutcomeTest {

    @Test
    void mostConservativeTakesTheHigherOrdinal() {
        assertThat(DecisionOutcome.mostConservative(DecisionOutcome.APPROVE, DecisionOutcome.REFER))
                .isEqualTo(DecisionOutcome.REFER);
        assertThat(DecisionOutcome.mostConservative(DecisionOutcome.REFER, DecisionOutcome.DECLINE))
                .isEqualTo(DecisionOutcome.DECLINE);
        assertThat(DecisionOutcome.mostConservative(DecisionOutcome.DECLINE, DecisionOutcome.APPROVE))
                .isEqualTo(DecisionOutcome.DECLINE);
    }

    @Test
    void isSymmetricAndIdempotent() {
        assertThat(DecisionOutcome.mostConservative(DecisionOutcome.APPROVE, DecisionOutcome.DECLINE))
                .isEqualTo(DecisionOutcome.mostConservative(DecisionOutcome.DECLINE, DecisionOutcome.APPROVE));
        assertThat(DecisionOutcome.mostConservative(DecisionOutcome.REFER, DecisionOutcome.REFER))
                .isEqualTo(DecisionOutcome.REFER);
    }

    @Test
    void orderingIsApproveReferDecline() {
        assertThat(DecisionOutcome.APPROVE.ordinal()).isLessThan(DecisionOutcome.REFER.ordinal());
        assertThat(DecisionOutcome.REFER.ordinal()).isLessThan(DecisionOutcome.DECLINE.ordinal());
    }
}
