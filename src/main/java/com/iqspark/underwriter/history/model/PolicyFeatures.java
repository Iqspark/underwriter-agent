package com.iqspark.underwriter.history.model;

import com.iqspark.underwriter.domain.model.Submission;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A uniform feature view extracted from either a {@link Submission} or a {@link HistoricalPolicy},
 * so the similarity model can compare them directly. Numeric features are normalized later by
 * {@link FeatureRanges}; categorical features are matched for equality.
 *
 * <p>Missing numerics default to 0 and missing categoricals to "" — neutral and consistent across
 * both sides of a comparison, so an irrelevant feature for a given line contributes no distance.
 */
public record PolicyFeatures(Map<String, Double> numeric, Map<String, String> categorical) {

    public static final List<String> NUMERIC_KEYS = List.of(
            "roofAgeYears", "vacancyMonths", "priorLossCount", "coverageAmount",
            "monitoredAlarm", "inspectionIntervalHours", "distanceToFireHallKm",
            "securitySystem", "units", "squareFeet", "yearBuilt");

    public static final List<String> CATEGORICAL_KEYS = List.of(
            "city", "province", "construction", "occupancyType");

    public double num(String key) {
        return numeric.getOrDefault(key, 0.0);
    }

    public String cat(String key) {
        return categorical.getOrDefault(key, "");
    }

    public static PolicyFeatures fromSubmission(Submission s) {
        Map<String, Double> n = new LinkedHashMap<>();
        Map<String, String> c = new LinkedHashMap<>();

        Submission.Building b = s.building();
        Submission.Vacancy v = s.vacancy();
        Submission.Protection p = s.protection();
        Submission.Applicant a = s.applicant();
        Submission.RiskLocation loc = s.location();

        n.put("roofAgeYears", b != null ? d(b.roofAgeYears()) : 0.0);
        n.put("vacancyMonths", monthsVacant(v));
        n.put("priorLossCount", a != null ? d(a.priorLossCount()) : 0.0);
        n.put("coverageAmount", s.requestedCoverage() != null ? s.requestedCoverage().amount() : 0.0);
        n.put("monitoredAlarm", p != null ? bool(p.monitoredAlarm()) : 0.0);
        n.put("inspectionIntervalHours", v != null ? d(v.inspectionIntervalHours()) : 0.0);
        n.put("distanceToFireHallKm", p != null ? d(p.distanceToFireHallKm()) : 0.0);
        n.put("securitySystem", v != null ? bool(v.securitySystem()) : 0.0);
        n.put("units", b != null ? d(b.units()) : 0.0);
        n.put("squareFeet", b != null ? d(b.squareFeet()) : 0.0);
        n.put("yearBuilt", b != null ? d(b.yearBuilt()) : 0.0);

        c.put("city", loc != null ? safe(loc.city()) : "");
        c.put("province", loc != null ? safe(loc.province()) : "");
        c.put("construction", b != null ? safe(b.construction()) : "");
        c.put("occupancyType", b != null ? safe(b.occupancyType()) : "");

        return new PolicyFeatures(n, c);
    }

    public static int monthsVacantValue(Submission.Vacancy v) {
        return (int) monthsVacant(v);
    }

    private static double monthsVacant(Submission.Vacancy v) {
        if (v == null || v.vacantSince() == null) {
            return 0.0;
        }
        long months = ChronoUnit.MONTHS.between(v.vacantSince(), LocalDate.now());
        return Math.max(0, months);
    }

    private static double d(Integer i) {
        return i == null ? 0.0 : i.doubleValue();
    }

    private static double bool(Boolean b) {
        return Boolean.TRUE.equals(b) ? 1.0 : 0.0;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
