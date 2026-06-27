package com.iqspark.underwriter.domain.decision;

/**
 * The recommended action. Ordered from least to most conservative so the orchestrator can take
 * the "most conservative" of two outcomes by comparing ordinals.
 */
public enum DecisionOutcome {
    APPROVE,
    REFER,
    DECLINE;

    /** The more conservative (higher-ordinal) of two outcomes. */
    public static DecisionOutcome mostConservative(DecisionOutcome a, DecisionOutcome b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
