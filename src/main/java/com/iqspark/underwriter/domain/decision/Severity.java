package com.iqspark.underwriter.domain.decision;

/**
 * Finding severity and its risk weight. The risk score of a decision is the sum of finding
 * weights. {@code KNOCKOUT} (100) forces a {@code DECLINE} and is excluded from the pricing load
 * so it does not distort the indicative premium.
 */
public enum Severity {
    INFO(0),
    LOW(1),
    MEDIUM(3),
    HIGH(6),
    KNOCKOUT(100);

    private final int weight;

    Severity(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
