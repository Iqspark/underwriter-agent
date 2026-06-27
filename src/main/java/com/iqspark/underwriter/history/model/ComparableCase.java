package com.iqspark.underwriter.history.model;

/**
 * A single comparable past policy surfaced as evidence behind a learned assessment — the
 * "show me the similar files" explainability of the case-based core.
 */
public record ComparableCase(
        String policyId,
        double similarity,
        String city,
        boolean hadClaim,
        Peril dominantPeril,
        double lossRatio,
        double ratePerThousand
) {}
