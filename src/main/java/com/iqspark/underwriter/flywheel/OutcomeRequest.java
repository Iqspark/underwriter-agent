package com.iqspark.underwriter.flywheel;

/** Request to record a realized outcome for a decision (from the PAS/claims feed). */
public record OutcomeRequest(String reference, boolean hadClaim, double lossRatio) {}
