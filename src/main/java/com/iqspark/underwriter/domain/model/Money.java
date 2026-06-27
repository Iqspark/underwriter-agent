package com.iqspark.underwriter.domain.model;

/**
 * A simple money value object. Amounts are held as a {@code double} for the indicative premium /
 * requested coverage use cases in this service; a production rating engine would use BigDecimal.
 */
public record Money(double amount, String currency) {

    public static Money cad(double amount) {
        return new Money(amount, "CAD");
    }

    public static Money of(double amount, String currency) {
        return new Money(amount, currency == null || currency.isBlank() ? "CAD" : currency);
    }
}
