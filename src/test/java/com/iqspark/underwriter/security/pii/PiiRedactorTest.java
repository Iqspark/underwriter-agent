package com.iqspark.underwriter.security.pii;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiRedactorTest {

    @Test
    void redactsEmailsPostalCodesAndLongNumbers() {
        String in = "Contact jane.doe@example.com at P3A 1A1, policy 1234567";
        String out = PiiRedactor.redact(in);
        assertThat(out).doesNotContain("jane.doe@example.com", "P3A 1A1", "1234567");
        assertThat(out).contains("[redacted-email]", "[redacted-postal]", "[redacted-number]");
    }

    @Test
    void keepsShortNumbersLikeRiskScores() {
        assertThat(PiiRedactor.redact("risk weight=16 premium 3477")).contains("16", "3477");
    }

    @Test
    void masksNamesToInitials() {
        assertThat(PiiRedactor.maskName("John Smith")).isEqualTo("J*** S***");
        assertThat(PiiRedactor.maskAddress("55 Maple Lane")).isEqualTo("[redacted-address]");
    }

    @Test
    void nullSafe() {
        assertThat(PiiRedactor.redact(null)).isNull();
        assertThat(PiiRedactor.maskName(null)).isNull();
    }
}
