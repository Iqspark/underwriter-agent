package com.iqspark.underwriter.rules.config;

/**
 * One condition of a rule: a {@code fact} compared with an {@code op}, optionally against a
 * {@code value}. Ops: eq, ne, gt, gte, lt, lte, eqIgnoreCase, isTrue, isFalse, isNull, isNotNull.
 */
public record Condition(String fact, String op, Object value) {}
