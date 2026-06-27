package com.iqspark.underwriter.security.authority;

import com.iqspark.underwriter.persistence.DecisionStore;
import com.iqspark.underwriter.persistence.StoredDecision;
import com.iqspark.underwriter.security.AppRoles;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BindingServiceTest {

    private final DecisionStore store = mock(DecisionStore.class);
    private final AuthorityService authority = new AuthorityService(1_000_000, 10_000_000, 5_000_000);
    private final BindingService binding = new BindingService(store, authority);

    private StoredDecision decision(String ref, String outcome, double coverage) {
        return new StoredDecision(ref, "VACANT_HOME", outcome, 0, coverage, null, "r",
                List.of(), List.of(), null, Instant.now(), List.of(), true);
    }

    @Test
    void withinAuthorityBindsOnSingleApproval() {
        when(store.find("R1")).thenReturn(Optional.of(decision("R1", "APPROVE", 100_000)));
        BindingStatus s = binding.submitApproval("R1", "uw", Set.of(AppRoles.UNDERWRITER), "APPROVE");
        assertThat(s.status()).isEqualTo(BindingStatus.BOUND);
        assertThat(s.requiredApprovals()).isEqualTo(1);
    }

    @Test
    void highValueNeedsTwoDistinctApprovers() {
        when(store.find("R2")).thenReturn(Optional.of(decision("R2", "APPROVE", 6_000_000)));
        BindingStatus first = binding.submitApproval("R2", "a", Set.of(AppRoles.SENIOR_UNDERWRITER), "APPROVE");
        assertThat(first.status()).isEqualTo(BindingStatus.PENDING);

        BindingStatus sameAgain = binding.submitApproval("R2", "a", Set.of(AppRoles.SENIOR_UNDERWRITER), "APPROVE");
        assertThat(sameAgain.status()).isEqualTo(BindingStatus.PENDING); // SoD: same approver doesn't count twice

        BindingStatus second = binding.submitApproval("R2", "b", Set.of(AppRoles.SENIOR_UNDERWRITER), "APPROVE");
        assertThat(second.status()).isEqualTo(BindingStatus.BOUND);
    }

    @Test
    void cannotPlainApproveADecline() {
        when(store.find("R3")).thenReturn(Optional.of(decision("R3", "DECLINE", 100_000)));
        assertThatThrownBy(() -> binding.submitApproval("R3", "uw", Set.of(AppRoles.UNDERWRITER), "APPROVE"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void overrideRequiresSenior() {
        when(store.find("R4")).thenReturn(Optional.of(decision("R4", "DECLINE", 100_000)));
        assertThatThrownBy(() -> binding.submitApproval("R4", "uw", Set.of(AppRoles.UNDERWRITER), "OVERRIDE"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void seniorOverrideBindsWithFourEyes() {
        when(store.find("R5")).thenReturn(Optional.of(decision("R5", "DECLINE", 100_000)));
        assertThat(binding.submitApproval("R5", "a", Set.of(AppRoles.SENIOR_UNDERWRITER), "OVERRIDE").status())
                .isEqualTo(BindingStatus.PENDING);
        assertThat(binding.submitApproval("R5", "b", Set.of(AppRoles.SENIOR_UNDERWRITER), "OVERRIDE").status())
                .isEqualTo(BindingStatus.BOUND);
    }

    @Test
    void missingDecisionThrows() {
        when(store.find("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> binding.submitApproval("nope", "uw", Set.of(AppRoles.UNDERWRITER), "APPROVE"))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
