package com.iqspark.underwriter.domain.decision;

/**
 * An advisory flag raised by the Reviewer agent (the LLM "skeptical underwriter", ADR-0022) when the
 * assembled decision is internally incoherent — e.g. the rationale downplays or omits a knockout.
 * Flags never change the outcome; they surface for a human and are recorded in the audit trail.
 *
 * @param code      stable flag code (e.g. RATIONALE_OMITS_KNOCKOUT)
 * @param severity  INFO | LOW | MEDIUM | HIGH (advisory weight, not a risk weight)
 * @param message   human-readable description
 * @param reference the finding code or claim the flag refers to
 */
public record ReviewFlag(String code, String severity, String message, String reference) {}
