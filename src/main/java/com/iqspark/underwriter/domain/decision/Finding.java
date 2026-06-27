package com.iqspark.underwriter.domain.decision;

/**
 * A single underwriting finding. Every finding is explainable: it carries a stable {@code code},
 * a {@link Severity}, a {@code category}, a human-readable {@code message} and {@code rationale},
 * and the {@code source} that raised it (a rule id or an agent name).
 */
public record Finding(
        String code,
        Severity severity,
        String category,
        String message,
        String rationale,
        String source
) {
    public int weight() {
        return severity.weight();
    }

    public boolean isKnockout() {
        return severity == Severity.KNOCKOUT;
    }
}
