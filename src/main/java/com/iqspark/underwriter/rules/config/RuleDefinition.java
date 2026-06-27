package com.iqspark.underwriter.rules.config;

import com.iqspark.underwriter.domain.decision.Severity;
import com.iqspark.underwriter.domain.model.LineOfBusiness;

import java.util.List;

/**
 * A declarative underwriting rule loaded from YAML. Fires when every {@link Condition} in
 * {@code all} holds. Scoping: an explicit {@code lines} list restricts applicability; otherwise a
 * single {@code line} (defaulted from the file name) applies; a shared rule with neither applies to
 * every line.
 */
public record RuleDefinition(
        String id,
        LineOfBusiness line,
        List<LineOfBusiness> lines,
        String code,
        String category,
        Severity severity,
        List<Condition> all,
        String message,
        String rationale
) {
    public boolean appliesTo(LineOfBusiness submissionLine) {
        if (lines != null && !lines.isEmpty()) {
            return lines.contains(submissionLine);
        }
        if (line != null) {
            return line == submissionLine;
        }
        return true; // shared, unrestricted
    }
}
