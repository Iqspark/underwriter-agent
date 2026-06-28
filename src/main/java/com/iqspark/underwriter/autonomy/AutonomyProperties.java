package com.iqspark.underwriter.autonomy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Autonomy/STP bounds ({@code underwriter.autonomy.*}). These encode appetite and are
 * <b>illustrative starting points on synthetic data</b> — calibrate against real loss experience
 * and confirm with UW/actuarial via the model-governance gate before enabling straight-through.
 */
@Component
@ConfigurationProperties("underwriter.autonomy")
public class AutonomyProperties {

    /** Master switch for tier routing. */
    private boolean enabled = true;
    /** Max coverage eligible for straight-through auto-approval. */
    private double maxCoverage = 750_000;
    /** Coverage above this routes to a specialist (large/edge risk). */
    private double specialistCoverage = 2_000_000;
    /** Learned claim probability must be below this for AUTO. */
    private double claimProbabilityMax = 0.15;
    /** Learned expected loss ratio must be below this for AUTO. */
    private double lossRatioMax = 0.5;
    /** Require learning confidence HIGH for AUTO. */
    private boolean requireHighConfidence = true;
    /** Fraction of auto-approvals sampled for QA (1.0 = 100% initially; taper later). */
    private double qaSampleRate = 1.0;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public double getMaxCoverage() { return maxCoverage; }
    public void setMaxCoverage(double maxCoverage) { this.maxCoverage = maxCoverage; }
    public double getSpecialistCoverage() { return specialistCoverage; }
    public void setSpecialistCoverage(double specialistCoverage) { this.specialistCoverage = specialistCoverage; }
    public double getClaimProbabilityMax() { return claimProbabilityMax; }
    public void setClaimProbabilityMax(double claimProbabilityMax) { this.claimProbabilityMax = claimProbabilityMax; }
    public double getLossRatioMax() { return lossRatioMax; }
    public void setLossRatioMax(double lossRatioMax) { this.lossRatioMax = lossRatioMax; }
    public boolean isRequireHighConfidence() { return requireHighConfidence; }
    public void setRequireHighConfidence(boolean requireHighConfidence) { this.requireHighConfidence = requireHighConfidence; }
    public double getQaSampleRate() { return qaSampleRate; }
    public void setQaSampleRate(double qaSampleRate) { this.qaSampleRate = qaSampleRate; }
}
