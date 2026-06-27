package com.iqspark.underwriter.domain.model;

import java.time.LocalDate;

/**
 * An underwriting submission. Immutable record graph; Jackson serializes it to/from JSON.
 *
 * <p>Nullable fields are intentional — missing data is a signal the completeness rules detect,
 * not something defaulted away. A {@code VACANT_HOME} submission carries a {@link Vacancy} section;
 * a {@code RENTAL} submission carries a {@link Rental} section; a {@code CONTENTS} submission
 * carries a {@link Contents} section (and may omit {@link Building}).
 */
public record Submission(
        String reference,
        LineOfBusiness lineOfBusiness,
        Applicant applicant,
        RiskLocation location,
        Building building,
        Vacancy vacancy,
        Protection protection,
        Rental rental,
        Contents contents,
        Money requestedCoverage
) {

    /** The declared line, or the default ({@code VACANT_HOME}) when none is supplied. */
    public LineOfBusiness effectiveLine() {
        return lineOfBusiness != null ? lineOfBusiness : LineOfBusiness.DEFAULT;
    }

    public record Applicant(
            String name,
            String brokerName,
            Boolean priorLossesDeclared,
            Integer priorLossCount
    ) {}

    public record RiskLocation(
            String addressLine,
            String city,
            String province,
            String postalCode,
            Double latitude,
            Double longitude
    ) {}

    public record Building(
            String construction,
            String occupancyType,
            Integer units,
            Integer squareFeet,
            Integer yearBuilt,
            Integer roofAgeYears,
            Boolean renovationPlanned,
            Boolean demolitionPlanned
    ) {}

    public record Vacancy(
            LocalDate vacantSince,
            Integer inspectionIntervalHours,
            Boolean utilitiesOn,
            Boolean waterShutOff,
            Boolean securitySystem
    ) {}

    public record Protection(
            Boolean monitoredAlarm,
            Boolean sprinklered,
            Integer distanceToHydrantMeters,
            Integer distanceToFireHallKm
    ) {}

    public record Rental(
            String tenancyType,                // "LONG_TERM" | "SHORT_TERM"
            Boolean shortTermRentalEndorsed,
            Long liabilityLimit,
            Boolean tenantScreening
    ) {}

    public record Contents(
            Long contentsValue,
            Long highValueItemsValue,
            Boolean highValueItemsScheduled,
            Boolean replacementCost,
            Boolean securityDevice
    ) {}
}
