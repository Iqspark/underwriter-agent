package com.iqspark.underwriter.dashboard;

import com.iqspark.underwriter.flywheel.RealizedOutcomeRepository;
import com.iqspark.underwriter.persistence.DecisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes the embedded underwriting-performance dashboard from the system of record (decisions) and
 * the flywheel (realized outcomes). Read-only; the same numbers are also exported as Micrometer
 * metrics for Grafana.
 */
@Service
public class DashboardService {

    private final DecisionRepository decisions;
    private final RealizedOutcomeRepository outcomes;

    public DashboardService(DecisionRepository decisions, RealizedOutcomeRepository outcomes) {
        this.decisions = decisions;
        this.outcomes = outcomes;
    }

    @Transactional(readOnly = true)
    public DashboardView snapshot() {
        long total = decisions.count();
        long approve = decisions.countByOutcome("APPROVE");
        long refer = decisions.countByOutcome("REFER");
        long decline = decisions.countByOutcome("DECLINE");
        long auto = decisions.countByTier("AUTO");
        long assisted = decisions.countByTier("ASSISTED");
        long specialist = decisions.countByTier("SPECIALIST");

        long outcomeCount = outcomes.count();
        long claims = outcomes.countByHadClaimTrue();

        double stpRate = ratio(auto, total);
        double realizedClaimRate = ratio(claims, outcomeCount);
        double coverage = ratio(outcomeCount, total);

        return new DashboardView(
                total, approve, refer, decline, auto, assisted, specialist,
                round(stpRate),
                round(nz(decisions.averagePremium())),
                round(nz(decisions.averagePredictedClaimProbability())),
                outcomeCount,
                round(realizedClaimRate),
                round(nz(outcomes.averageLossRatio())),
                round(coverage));
    }

    private static double ratio(long numerator, long denominator) {
        return denominator > 0 ? (double) numerator / denominator : 0.0;
    }

    private static double nz(Double v) {
        return v == null ? 0.0 : v;
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
