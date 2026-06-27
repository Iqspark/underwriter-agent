package com.iqspark.underwriter.rules;

import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.geo.GeoService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Flattens a {@link Submission} into named facts and computes the few derived values rules can't
 * express declaratively — geo remoteness (via {@link GeoService}), months vacant, coverage per
 * square foot, and presence/contradiction flags. The {@code ConfigurableRulesEngine} evaluates the
 * YAML rules against these facts.
 */
@Component
public class FactExtractor {

    private final GeoService geoService;

    public FactExtractor(GeoService geoService) {
        this.geoService = geoService;
    }

    public Map<String, Object> extract(Submission s) {
        Map<String, Object> f = new HashMap<>();

        // ---- Reference / applicant ----
        f.put("missingReference", isBlank(s.reference()));

        Submission.Applicant a = s.applicant();
        f.put("missingApplicantName", a == null || isBlank(a.name()));
        f.put("priorLossesDeclared", a != null && Boolean.TRUE.equals(a.priorLossesDeclared()));
        f.put("priorLossCount", a != null && a.priorLossCount() != null ? a.priorLossCount() : 0);

        // ---- Location & remoteness ----
        Submission.RiskLocation loc = s.location();
        boolean locationPresent = loc != null && (!isBlank(loc.city()) || !isBlank(loc.province()));
        f.put("locationPresent", locationPresent);
        f.put("missingLocation", loc == null || isBlank(loc.city()) || isBlank(loc.province()));

        GeoService.RemotenessResult geo = loc == null
                ? GeoService.RemotenessResult.unresolved()
                : geoService.assess(loc.latitude(), loc.longitude(), loc.province());
        f.put("locationResolved", geo.resolved());
        f.put("remote", geo.resolved() && geo.remote());
        f.put("nearestDistanceKm", Double.isNaN(geo.nearestDistanceKm())
                ? 0L : Math.round(geo.nearestDistanceKm()));
        f.put("nearestCity", geo.nearestCity() == null ? "" : geo.nearestCity());

        // ---- Building ----
        Submission.Building b = s.building();
        f.put("missingBuilding", b == null);
        f.put("occupancy", b != null && b.occupancyType() != null ? b.occupancyType() : "");
        f.put("units", b != null && b.units() != null ? b.units() : 0);
        f.put("squareFeet", b != null && b.squareFeet() != null ? b.squareFeet() : 0);
        f.put("yearBuilt", b != null && b.yearBuilt() != null ? b.yearBuilt() : 0);
        f.put("roofAgeYears", b != null && b.roofAgeYears() != null ? b.roofAgeYears() : 0);
        f.put("demolitionPlanned", b != null && Boolean.TRUE.equals(b.demolitionPlanned()));
        f.put("renovationPlanned", b != null && Boolean.TRUE.equals(b.renovationPlanned()));

        // coverage per square foot
        double coverage = s.requestedCoverage() != null ? s.requestedCoverage().amount() : 0;
        int sqft = b != null && b.squareFeet() != null ? b.squareFeet() : 0;
        f.put("perSqft", sqft > 0 ? Math.round(coverage / sqft) : 0L);

        // ---- Vacancy (vacant-home line) ----
        Submission.Vacancy v = s.vacancy();
        f.put("vacancyPresent", v != null);
        f.put("missingVacantSince", v == null || v.vacantSince() == null);
        f.put("missingInspectionInterval", v == null || v.inspectionIntervalHours() == null);
        f.put("inspectionIntervalHours", v != null && v.inspectionIntervalHours() != null
                ? v.inspectionIntervalHours() : 0);
        f.put("monthsVacant", monthsVacant(v));
        f.put("utilitiesOn", v != null ? nullableBool(v.utilitiesOn()) : null);
        f.put("waterShutOff", v != null ? nullableBool(v.waterShutOff()) : null);
        f.put("securitySystem", v != null ? nullableBool(v.securitySystem()) : null);

        // ---- Protection ----
        Submission.Protection p = s.protection();
        f.put("protectionPresent", p != null);
        f.put("monitoredAlarm", p != null ? nullableBool(p.monitoredAlarm()) : null);
        f.put("sprinklered", p != null && Boolean.TRUE.equals(p.sprinklered()));
        f.put("hydrantMeters", p != null && p.distanceToHydrantMeters() != null
                ? p.distanceToHydrantMeters() : 0);
        f.put("fireHallKm", p != null && p.distanceToFireHallKm() != null
                ? p.distanceToFireHallKm() : 0);

        // ---- Rental line ----
        Submission.Rental r = s.rental();
        boolean shortTerm = r != null && "SHORT_TERM".equalsIgnoreCase(r.tenancyType());
        f.put("shortTermRental", shortTerm);
        f.put("shortTermRentalEndorsed", r != null && Boolean.TRUE.equals(r.shortTermRentalEndorsed()));
        f.put("liabilityLimitMissing", r == null || r.liabilityLimit() == null);
        f.put("liabilityLimit", r != null && r.liabilityLimit() != null ? r.liabilityLimit() : 0L);
        f.put("noTenantScreening", r != null && !Boolean.TRUE.equals(r.tenantScreening()));

        // ---- Contents line ----
        Submission.Contents cont = s.contents();
        f.put("contentsValueMissing", cont == null || cont.contentsValue() == null);
        f.put("highValueItemsValue", cont != null && cont.highValueItemsValue() != null
                ? cont.highValueItemsValue() : 0L);
        f.put("highValueItemsScheduled", cont != null && Boolean.TRUE.equals(cont.highValueItemsScheduled()));
        f.put("noSecurityDevice", cont != null && !Boolean.TRUE.equals(cont.securityDevice()));
        f.put("acvBasis", cont != null && !Boolean.TRUE.equals(cont.replacementCost()));

        return f;
    }

    private static long monthsVacant(Submission.Vacancy v) {
        if (v == null || v.vacantSince() == null) {
            return 0L;
        }
        return Math.max(0, ChronoUnit.MONTHS.between(v.vacantSince(), LocalDate.now()));
    }

    private static Boolean nullableBool(Boolean b) {
        return b; // preserve null so isTrue/isFalse only match an explicit value
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
