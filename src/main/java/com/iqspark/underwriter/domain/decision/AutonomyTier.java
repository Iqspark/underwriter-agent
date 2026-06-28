package com.iqspark.underwriter.domain.decision;

/**
 * How a decision should be handled (doc 7 §4.2 / doc 8 §4):
 *
 * <ul>
 *   <li>{@code AUTO} — straight-through: clean, low-risk, high-confidence files within tight bounds
 *       (auto-approve, sampled for QA).</li>
 *   <li>{@code ASSISTED} — the AI prepares everything; an underwriter reviews and decides.</li>
 *   <li>{@code SPECIALIST} — knockouts, low confidence, large/edge risks → routed to a senior.</li>
 * </ul>
 */
public enum AutonomyTier {
    AUTO,
    ASSISTED,
    SPECIALIST
}
