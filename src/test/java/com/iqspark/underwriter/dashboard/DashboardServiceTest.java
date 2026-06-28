package com.iqspark.underwriter.dashboard;

import com.iqspark.underwriter.flywheel.RealizedOutcomeRepository;
import com.iqspark.underwriter.persistence.DecisionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private final DecisionRepository decisions = mock(DecisionRepository.class);
    private final RealizedOutcomeRepository outcomes = mock(RealizedOutcomeRepository.class);
    private final DashboardService service = new DashboardService(decisions, outcomes);

    @Test
    void computesKpisFromDecisionsAndOutcomes() {
        when(decisions.count()).thenReturn(10L);
        when(decisions.countByOutcome("APPROVE")).thenReturn(6L);
        when(decisions.countByOutcome("REFER")).thenReturn(3L);
        when(decisions.countByOutcome("DECLINE")).thenReturn(1L);
        when(decisions.countByTier("AUTO")).thenReturn(4L);
        when(decisions.countByTier("ASSISTED")).thenReturn(5L);
        when(decisions.countByTier("SPECIALIST")).thenReturn(1L);
        when(decisions.averagePremium()).thenReturn(2500.0);
        when(decisions.averagePredictedClaimProbability()).thenReturn(0.20);
        when(outcomes.count()).thenReturn(5L);
        when(outcomes.countByHadClaimTrue()).thenReturn(2L);
        when(outcomes.averageLossRatio()).thenReturn(0.80);

        DashboardView v = service.snapshot();

        assertThat(v.totalDecisions()).isEqualTo(10);
        assertThat(v.approveCount()).isEqualTo(6);
        assertThat(v.stpRate()).isEqualTo(0.4);            // 4/10
        assertThat(v.realizedClaimRate()).isEqualTo(0.4);  // 2/5
        assertThat(v.outcomeCoverage()).isEqualTo(0.5);    // 5/10
        assertThat(v.averagePremium()).isEqualTo(2500.0);
        assertThat(v.averagePredictedClaimProbability()).isEqualTo(0.20);
    }

    @Test
    void emptyBookHasNoDivisionByZero() {
        when(decisions.count()).thenReturn(0L);
        when(outcomes.count()).thenReturn(0L);

        DashboardView v = service.snapshot();

        assertThat(v.totalDecisions()).isZero();
        assertThat(v.stpRate()).isZero();
        assertThat(v.outcomeCoverage()).isZero();
    }
}
