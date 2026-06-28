package com.iqspark.underwriter.security.authority;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorityCheckTest {

    @Test
    void factoriesSetDecisionAndReason() {
        assertThat(AuthorityCheck.allowed("ok").decision()).isEqualTo(AuthorityCheck.Decision.ALLOWED_SINGLE);
        assertThat(AuthorityCheck.fourEyes("dual").decision()).isEqualTo(AuthorityCheck.Decision.REQUIRES_FOUR_EYES);
        assertThat(AuthorityCheck.denied("no").isDenied()).isTrue();
        assertThat(AuthorityCheck.denied("no").reason()).isEqualTo("no");
    }

    @Test
    void requiredApprovalsIsTwoOnlyForFourEyes() {
        assertThat(AuthorityCheck.allowed("ok").requiredApprovals()).isEqualTo(1);
        assertThat(AuthorityCheck.denied("no").requiredApprovals()).isEqualTo(1);
        assertThat(AuthorityCheck.fourEyes("dual").requiredApprovals()).isEqualTo(2);
    }
}
