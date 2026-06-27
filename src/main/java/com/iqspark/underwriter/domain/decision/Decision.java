package com.iqspark.underwriter.domain.decision;

import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.history.model.LearnedAssessment;

import java.time.Instant;
import java.util.List;

/**
 * The decision the agent recommends for a submission. Advisory only — a human underwriter owns
 * the bind. Ships with the findings, conditions, indicative premium, the learned assessment
 * (comparable cases + prediction) and the ordered audit trail that justify it.
 */
public record Decision(
        String reference,
        DecisionOutcome outcome,
        int riskScore,
        List<Finding> findings,
        List<String> conditions,
        Money indicativePremium,
        String rationale,
        LearnedAssessment learnedAssessment,
        List<RetrievedSource> retrievedSources,
        List<String> auditTrail,
        Instant decidedAt
) {}
