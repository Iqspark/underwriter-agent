package com.iqspark.underwriter.support;

import com.iqspark.underwriter.domain.model.LineOfBusiness;
import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.domain.model.Submission;

import java.time.LocalDate;

/** Test fixtures: representative submissions across lines and outcomes. */
public final class Submissions {

    private Submissions() {
    }

    /** Clean, low-risk vacant-home file in Toronto — should not decline. */
    public static Submission vacantClean() {
        return new Submission(
                "T-CLEAN-1", LineOfBusiness.VACANT_HOME,
                new Submission.Applicant("Jardine Family Trust", "Downtown", false, 0),
                new Submission.RiskLocation("210 King St W", "Toronto", "ON", "M5H 1J8", 43.6480, -79.3860),
                new Submission.Building("Masonry", "Detached Home", 1, 2400, 2012, 6, false, false),
                new Submission.Vacancy(LocalDate.now().minusMonths(2), 24, true, true, true),
                new Submission.Protection(true, true, 40, 3),
                null, null, Money.cad(900_000), null);
    }

    /** Vacant-home file breaching the 72-hour inspection condition precedent — a knockout. */
    public static Submission vacantKnockout() {
        return new Submission(
                "T-KO-1", LineOfBusiness.VACANT_HOME,
                new Submission.Applicant("Hillside Holdings Ltd.", "Prairie", true, 3),
                new Submission.RiskLocation("55 Maple Lane", "Toronto", "ON", "M5H 1A1", 43.6532, -79.3832),
                new Submission.Building("Frame", "Detached Home", 1, 1800, 1955, 30, false, false),
                new Submission.Vacancy(LocalDate.now().minusMonths(12), 168, true, false, false),
                new Submission.Protection(false, false, 400, 25),
                null, null, Money.cad(300_000), null);
    }

    /** Remote, old, unsecured, repeatedly-lossy vacant-home file — high risk, should not approve. */
    public static Submission vacantRemoteRisky() {
        return new Submission(
                "T-RISK-1", LineOfBusiness.VACANT_HOME,
                new Submission.Applicant("Hinterland Inc.", "North", true, 3),
                new Submission.RiskLocation("Rural Route 7", "Flin Flon", "MB", null, 54.7682, -101.8650),
                new Submission.Building("Frame", "Detached Home", 1, 1800, 1955, 30, false, false),
                new Submission.Vacancy(LocalDate.now().minusMonths(18), 48, true, false, false),
                new Submission.Protection(false, false, 400, 25),
                null, null, Money.cad(300_000),
                "Inspection report: roof patched but underlying deck shows rot; signs of deferred "
                        + "maintenance throughout. Possible mold near the basement. A recent break-in "
                        + "was reported nearby. Inspector sentiment: poor condition overall.");
    }

    /** Short-term rental with no STR endorsement — a knockout on the rental line. */
    public static Submission rentalStrNoEndorsement() {
        return new Submission(
                "T-RENT-1", LineOfBusiness.RENTAL,
                new Submission.Applicant("Landlord Holdings Inc.", "Downtown", false, 0),
                new Submission.RiskLocation("120 King St W", "Toronto", "ON", "M5H 1A9", 43.6480, -79.3860),
                new Submission.Building("Masonry", "Apartment", 4, 5200, 2001, 9, false, false),
                null, null,
                new Submission.Rental("SHORT_TERM", false, 2_000_000L, true),
                null, Money.cad(950_000), null);
    }

    /** Contents file with unscheduled high-value items. */
    public static Submission contents() {
        return new Submission(
                "T-CONT-1", LineOfBusiness.CONTENTS,
                new Submission.Applicant("Jordan Tenant", "Downtown", false, 0),
                new Submission.RiskLocation("55 Queen St W #1203", "Toronto", "ON", "M5H 2M9", 43.6510, -79.3470),
                null, null, null, null,
                new Submission.Contents(90_000L, 40_000L, false, true, true),
                Money.cad(90_000), null);
    }

    /** A largely empty submission — should raise completeness findings and refer. */
    public static Submission missingMost() {
        return new Submission(
                null, LineOfBusiness.VACANT_HOME,
                null, null, null, null, null, null, null, null, null);
    }
}
