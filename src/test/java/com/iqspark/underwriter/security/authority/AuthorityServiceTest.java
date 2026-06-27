package com.iqspark.underwriter.security.authority;

import com.iqspark.underwriter.security.AppRoles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorityServiceTest {

    private final AuthorityService service = new AuthorityService(1_000_000, 10_000_000, 5_000_000);

    @Test
    void withinAuthorityNeedsSingleApproval() {
        AuthorityCheck c = service.evaluate(Set.of(AppRoles.UNDERWRITER), 100_000, false);
        assertThat(c.decision()).isEqualTo(AuthorityCheck.Decision.ALLOWED_SINGLE);
        assertThat(c.requiredApprovals()).isEqualTo(1);
    }

    @Test
    void coverageBeyondAuthorityIsDenied() {
        assertThat(service.evaluate(Set.of(AppRoles.UNDERWRITER), 2_000_000, false).isDenied()).isTrue();
    }

    @Test
    void highValueRequiresFourEyes() {
        AuthorityCheck c = service.evaluate(Set.of(AppRoles.SENIOR_UNDERWRITER), 6_000_000, false);
        assertThat(c.decision()).isEqualTo(AuthorityCheck.Decision.REQUIRES_FOUR_EYES);
        assertThat(c.requiredApprovals()).isEqualTo(2);
    }

    @Test
    void overridingADeclineNeedsSeniorAndFourEyes() {
        assertThat(service.evaluate(Set.of(AppRoles.UNDERWRITER), 100_000, true).isDenied()).isTrue();
        AuthorityCheck senior = service.evaluate(Set.of(AppRoles.SENIOR_UNDERWRITER), 100_000, true);
        assertThat(senior.decision()).isEqualTo(AuthorityCheck.Decision.REQUIRES_FOUR_EYES);
    }

    @Test
    void roleWithoutBindingAuthorityIsDenied() {
        assertThat(service.evaluate(Set.of(AppRoles.AUDITOR), 1_000, false).isDenied()).isTrue();
    }
}
