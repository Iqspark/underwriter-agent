package com.iqspark.underwriter.history.model;

import com.iqspark.underwriter.domain.model.LineOfBusiness;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One past policy in the book of business, with its risk features and how it actually performed
 * (claim / loss ratio / dominant peril). Synthetic today; a real policy+claims source in
 * production. Exposes a {@link PolicyFeatures} view for the similarity model.
 */
public record HistoricalPolicy(
        String id,
        LineOfBusiness line,
        String city,
        String province,
        String construction,
        String occupancyType,
        int units,
        int squareFeet,
        int yearBuilt,
        int roofAgeYears,
        int vacancyMonths,
        int inspectionIntervalHours,
        int distanceToFireHallKm,
        boolean securitySystem,
        boolean monitoredAlarm,
        int priorLossCount,
        double coverageAmount,
        boolean hadClaim,
        double lossRatio,
        double ratePerThousand,
        Peril dominantPeril
) {

    public PolicyFeatures features() {
        Map<String, Double> n = new LinkedHashMap<>();
        Map<String, String> c = new LinkedHashMap<>();

        n.put("roofAgeYears", (double) roofAgeYears);
        n.put("vacancyMonths", (double) vacancyMonths);
        n.put("priorLossCount", (double) priorLossCount);
        n.put("coverageAmount", coverageAmount);
        n.put("monitoredAlarm", monitoredAlarm ? 1.0 : 0.0);
        n.put("inspectionIntervalHours", (double) inspectionIntervalHours);
        n.put("distanceToFireHallKm", (double) distanceToFireHallKm);
        n.put("securitySystem", securitySystem ? 1.0 : 0.0);
        n.put("units", (double) units);
        n.put("squareFeet", (double) squareFeet);
        n.put("yearBuilt", (double) yearBuilt);

        c.put("city", city == null ? "" : city);
        c.put("province", province == null ? "" : province);
        c.put("construction", construction == null ? "" : construction);
        c.put("occupancyType", occupancyType == null ? "" : occupancyType);

        return new PolicyFeatures(n, c);
    }
}
