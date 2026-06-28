package com.iqspark.underwriter.runtime;

/**
 * The durable lifecycle of a submission case (doc 10 §2):
 * {@code RECEIVED → ASSESSING → (AUTO_DECIDED | REFERRED) → BOUND/CLOSED}, with {@code FAILED} for
 * cases that exhaust retries (dead-letter). Persisted in Postgres/H2 — not a stack frame — so HITL
 * and long-running steps survive restarts.
 */
public enum CaseStatus {
    RECEIVED,
    ASSESSING,
    AUTO_DECIDED,
    REFERRED,
    BOUND,
    CLOSED,
    FAILED
}
