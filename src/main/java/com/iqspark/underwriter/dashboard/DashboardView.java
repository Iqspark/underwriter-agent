package com.iqspark.underwriter.dashboard;

/**
 * Curated underwriting-performance KPIs (doc 10 §4): decision mix, straight-through rate, premium,
 * and the flywheel calibration signal (predicted claim probability vs realized claim rate).
 */
public record DashboardView(
        long totalDecisions,
        long approveCount,
        long referCount,
        long declineCount,
        long autoCount,
        long assistedCount,
        long specialistCount,
        double stpRate,
        double averagePremium,
        double averagePredictedClaimProbability,
        long outcomesRecorded,
        double realizedClaimRate,
        double averageRealizedLossRatio,
        double outcomeCoverage
) {}
