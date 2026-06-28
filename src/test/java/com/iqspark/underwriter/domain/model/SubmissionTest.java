package com.iqspark.underwriter.domain.model;

import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionTest {

    @Test
    void effectiveLineDefaultsToVacantHomeWhenNull() {
        Submission s = new Submission("R", null, null, null, null, null, null, null, null, null, null);
        assertThat(s.effectiveLine()).isEqualTo(LineOfBusiness.VACANT_HOME);
    }

    @Test
    void effectiveLineHonoursDeclaredLine() {
        assertThat(Submissions.rentalStrNoEndorsement().effectiveLine()).isEqualTo(LineOfBusiness.RENTAL);
        assertThat(Submissions.contents().effectiveLine()).isEqualTo(LineOfBusiness.CONTENTS);
    }

    @Test
    void moneyFactoriesDefaultCurrency() {
        assertThat(Money.cad(100).currency()).isEqualTo("CAD");
        assertThat(Money.of(100, null).currency()).isEqualTo("CAD");
        assertThat(Money.of(100, "USD").currency()).isEqualTo("USD");
    }
}
