package com.iqspark.underwriter.extraction;

import com.iqspark.underwriter.domain.model.LineOfBusiness;
import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.domain.model.Submission;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses a quote-summary document of {@code key: value} lines into a {@link Submission}. Recognised
 * keys are snake_case (see the API spec §3.3). Unrecognised or absent keys are left null on purpose
 * so the completeness rules can flag them — the extractor never invents data.
 */
@Component
public class KeyValueDocumentExtractor implements DocumentExtractor {

    @Override
    public Submission extract(String rawText) {
        Map<String, String> kv = parse(rawText);

        Submission.Applicant applicant = null;
        if (hasAny(kv, "applicant", "broker", "prior_losses_declared", "prior_loss_count")) {
            applicant = new Submission.Applicant(
                    kv.get("applicant"),
                    kv.get("broker"),
                    bool(kv.get("prior_losses_declared")),
                    integer(kv.get("prior_loss_count")));
        }

        Submission.RiskLocation location = null;
        if (hasAny(kv, "address", "city", "province", "postal_code", "latitude", "longitude")) {
            location = new Submission.RiskLocation(
                    kv.get("address"),
                    kv.get("city"),
                    kv.get("province"),
                    kv.get("postal_code"),
                    decimal(kv.get("latitude")),
                    decimal(kv.get("longitude")));
        }

        Submission.Building building = null;
        if (hasAny(kv, "construction", "occupancy", "units", "square_feet", "year_built",
                "roof_age", "renovation_planned", "demolition_planned")) {
            building = new Submission.Building(
                    kv.get("construction"),
                    kv.get("occupancy"),
                    integer(kv.get("units")),
                    integer(kv.get("square_feet")),
                    integer(kv.get("year_built")),
                    integer(kv.get("roof_age")),
                    bool(kv.get("renovation_planned")),
                    bool(kv.get("demolition_planned")));
        }

        Submission.Vacancy vacancy = null;
        if (hasAny(kv, "vacant_since", "inspection_interval_hours", "utilities_on",
                "water_shut_off", "security_system")) {
            vacancy = new Submission.Vacancy(
                    date(kv.get("vacant_since")),
                    integer(kv.get("inspection_interval_hours")),
                    bool(kv.get("utilities_on")),
                    bool(kv.get("water_shut_off")),
                    bool(kv.get("security_system")));
        }

        Submission.Protection protection = null;
        if (hasAny(kv, "monitored_alarm", "sprinklered", "hydrant_distance_m", "fire_hall_distance_km")) {
            protection = new Submission.Protection(
                    bool(kv.get("monitored_alarm")),
                    bool(kv.get("sprinklered")),
                    integer(kv.get("hydrant_distance_m")),
                    integer(kv.get("fire_hall_distance_km")));
        }

        Money coverage = null;
        Double amount = decimal(kv.get("requested_coverage"));
        if (amount != null) {
            coverage = Money.cad(amount);
        }

        return new Submission(
                kv.get("reference"),
                line(kv.get("line_of_business")),
                applicant, location, building, vacancy, protection,
                null, null, coverage, kv.get("notes"));
    }

    private Map<String, String> parse(String rawText) {
        Map<String, String> kv = new HashMap<>();
        if (rawText == null) {
            return kv;
        }
        for (String line : rawText.split("\\r?\\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = line.substring(0, colon).trim().toLowerCase().replace(' ', '_');
            String value = line.substring(colon + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                kv.put(key, value);
            }
        }
        return kv;
    }

    private static boolean hasAny(Map<String, String> kv, String... keys) {
        for (String k : keys) {
            if (kv.containsKey(k)) {
                return true;
            }
        }
        return false;
    }

    private static Boolean bool(String v) {
        if (v == null) {
            return null;
        }
        String s = v.trim().toLowerCase();
        return switch (s) {
            case "true", "yes", "y", "1" -> Boolean.TRUE;
            case "false", "no", "n", "0" -> Boolean.FALSE;
            default -> null;
        };
    }

    private static Integer integer(String v) {
        if (v == null) {
            return null;
        }
        try {
            return Integer.valueOf(v.trim().replaceAll("[,_$]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double decimal(String v) {
        if (v == null) {
            return null;
        }
        try {
            return Double.valueOf(v.trim().replaceAll("[,_$]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate date(String v) {
        if (v == null) {
            return null;
        }
        try {
            return LocalDate.parse(v.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LineOfBusiness line(String v) {
        if (v == null) {
            return null;
        }
        try {
            return LineOfBusiness.valueOf(v.trim().toUpperCase().replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
