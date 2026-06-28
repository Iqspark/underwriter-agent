package com.iqspark.underwriter.history;

import com.iqspark.underwriter.history.model.PolicyFeatures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LogisticRiskModelTest {

    private final HistoricalPolicyRepository repo = new HistoricalPolicyRepository(1500, 42);
    private final LogisticRiskModel model = new LogisticRiskModel(repo);

    private PolicyFeatures features(double roofAge, double vacancyMonths, double priorLoss,
                                   double inspectionHrs, double fireHallKm, double security) {
        Map<String, Double> n = Map.ofEntries(
                Map.entry("roofAgeYears", roofAge),
                Map.entry("vacancyMonths", vacancyMonths),
                Map.entry("priorLossCount", priorLoss),
                Map.entry("coverageAmount", 400_000.0),
                Map.entry("monitoredAlarm", 0.0),
                Map.entry("inspectionIntervalHours", inspectionHrs),
                Map.entry("distanceToFireHallKm", fireHallKm),
                Map.entry("securitySystem", security),
                Map.entry("units", 1.0),
                Map.entry("squareFeet", 2000.0),
                Map.entry("yearBuilt", 1970.0));
        return new PolicyFeatures(n, Map.of());
    }

    @Test
    void trainsAndPredictsInRange() {
        assertThat(model.isReady()).isTrue();
        assertThat(model.name()).isEqualTo("logistic");
        double p = model.predictClaimProbability(features(20, 12, 1, 48, 10, 1));
        assertThat(p).isBetween(0.0, 1.0);
    }

    @Test
    void higherRiskScoresHigherThanCleanRisk() {
        double high = model.predictClaimProbability(features(40, 36, 3, 168, 30, 0)); // old, long-vacant, lossy, unsecured
        double low = model.predictClaimProbability(features(3, 1, 0, 24, 1, 1));       // new, fresh, no losses, secured
        assertThat(high).isBetween(0.0, 1.0);
        assertThat(low).isBetween(0.0, 1.0);
        assertThat(high).isGreaterThan(low); // the model learned the risk direction from the book
    }

    @Test
    void unreadyOnTinyBook() {
        LogisticRiskModel tiny = new LogisticRiskModel(new HistoricalPolicyRepository(5, 42));
        assertThat(tiny.isReady()).isFalse();
        assertThat(tiny.predictClaimProbability(features(40, 36, 3, 168, 30, 0))).isEqualTo(0.0);
    }
}
