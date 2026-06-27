package com.iqspark.underwriter.history.model;

import java.util.List;

/**
 * The output of the AI-first learning core: what the comparable history predicts for a submission,
 * plus the evidence (comparable cases + area risk) and a confidence label. On a thin/absent book
 * the assessment is {@code coldStart} and the learned layer is treated as neutral.
 */
public record LearnedAssessment(
        int comparableCount,
        double meanSimilarity,
        double claimProbability,
        double expectedLossRatio,
        double suggestedRatePerThousand,
        Peril dominantPeril,
        String confidence,            // LOW | MEDIUM | HIGH
        boolean coldStart,
        List<ComparableCase> topComparables,
        AreaRiskStat areaRisk
) {

    /** A neutral assessment used when the book is too thin to learn from. */
    public static LearnedAssessment coldStart(AreaRiskStat areaRisk) {
        return new LearnedAssessment(
                0, 0.0, 0.0, 0.0, 0.0, Peril.NONE, "LOW", true, List.of(), areaRisk);
    }
}
