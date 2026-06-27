package com.iqspark.underwriter.history.model;

/** Per-area claim/theft statistics learned from the book, used for findings and the pricing load. */
public record AreaRiskStat(
        String city,
        int sampleSize,
        double overallClaimRate,
        double theftClaimRate,
        double avgLossRatio
) {
    /** A neutral stat for an area not present in the book. */
    public static AreaRiskStat unknown(String city) {
        return new AreaRiskStat(city, 0, 0.0, 0.0, 0.0);
    }
}
