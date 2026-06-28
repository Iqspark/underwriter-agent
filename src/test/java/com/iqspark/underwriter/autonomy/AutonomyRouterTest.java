package com.iqspark.underwriter.autonomy;

import com.iqspark.underwriter.domain.decision.AutonomyTier;
import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.ReviewFlag;
import com.iqspark.underwriter.domain.decision.Severity;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import com.iqspark.underwriter.history.model.Peril;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AutonomyRouterTest {

    private final AutonomyRouter router = new AutonomyRouter(new AutonomyProperties());

    private LearnedAssessment learned(String confidence, double claimProb, double lossRatio) {
        return new LearnedAssessment(20, 0.85, claimProb, lossRatio, 6.0, Peril.NONE,
                confidence, false, List.of(), null);
    }

    private Finding knockout() {
        return new Finding("INSPECTION_INTERVAL_BREACH", Severity.KNOCKOUT, "COMPLIANCE", "…", "…", "rule");
    }

    private Finding priorLosses() {
        return new Finding("PRIOR_LOSSES", Severity.HIGH, "MORAL_HAZARD", "2 prior losses", "…", "rule");
    }

    @Test
    void cleanLowRiskAutoApprovesAndIsQaSampled() {
        var a = router.evaluate(DecisionOutcome.APPROVE, List.of(), learned("HIGH", 0.05, 0.30),
                500_000, List.of());
        assertThat(a.tier()).isEqualTo(AutonomyTier.AUTO);
        assertThat(a.qaSampled()).isTrue(); // default sample rate 1.0
    }

    @Test
    void knockoutGoesToSpecialist() {
        var a = router.evaluate(DecisionOutcome.DECLINE, List.of(knockout()), learned("HIGH", 0.05, 0.3),
                500_000, List.of());
        assertThat(a.tier()).isEqualTo(AutonomyTier.SPECIALIST);
        assertThat(a.qaSampled()).isFalse();
    }

    @Test
    void largeCoverageIsNotAuto() {
        var a = router.evaluate(DecisionOutcome.APPROVE, List.of(), learned("HIGH", 0.05, 0.3),
                900_000, List.of());
        assertThat(a.tier()).isNotEqualTo(AutonomyTier.AUTO);
    }

    @Test
    void lowConfidenceGoesToSpecialist() {
        var a = router.evaluate(DecisionOutcome.APPROVE, List.of(), learned("LOW", 0.05, 0.3),
                500_000, List.of());
        assertThat(a.tier()).isEqualTo(AutonomyTier.SPECIALIST);
    }

    @Test
    void highClaimProbabilityIsNotAuto() {
        var a = router.evaluate(DecisionOutcome.APPROVE, List.of(), learned("HIGH", 0.50, 0.3),
                500_000, List.of());
        assertThat(a.tier()).isNotEqualTo(AutonomyTier.AUTO);
    }

    @Test
    void edgeRiskGoesToSpecialist() {
        var a = router.evaluate(DecisionOutcome.APPROVE, List.of(priorLosses()), learned("HIGH", 0.05, 0.3),
                500_000, List.of());
        assertThat(a.tier()).isEqualTo(AutonomyTier.SPECIALIST);
    }

    @Test
    void highReviewFlagGoesToSpecialist() {
        var a = router.evaluate(DecisionOutcome.APPROVE, List.of(), learned("HIGH", 0.05, 0.3),
                500_000, List.of(new ReviewFlag("X", "HIGH", "msg", "ref")));
        assertThat(a.tier()).isEqualTo(AutonomyTier.SPECIALIST);
    }
}
