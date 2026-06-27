package com.iqspark.underwriter.agent;

/**
 * A specialist agent in the underwriting pipeline. Each agent declares an {@link #order()} and
 * enriches the shared {@link UnderwritingContext}. The {@code DecisionOrchestrator} discovers all
 * agents and runs them in order.
 */
public interface UnderwritingAgent {

    /** Lower runs first (Intake 10, RiskProfiling 20, PatternLearning 25, Compliance 30, Pricing 40). */
    int order();

    /** Do this agent's work, recording findings/results and an audit entry on the context. */
    void handle(UnderwritingContext context);
}
