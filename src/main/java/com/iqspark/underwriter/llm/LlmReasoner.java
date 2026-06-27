package com.iqspark.underwriter.llm;

import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.RetrievedSource;
import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.history.model.LearnedAssessment;

import java.util.List;

/**
 * Turns the structured evidence of a decision into a human-readable rationale. The reasoner
 * <em>only explains</em>; it never makes or overrides a decision (ADR-0001). Pluggable and
 * offline by default (ADR-0003).
 */
public interface LlmReasoner {

    /** A short provider label recorded in the audit trail (e.g. "template-offline"). */
    String name();

    /**
     * Produce a rationale for the recommended outcome.
     *
     * @throws LlmReasoningException if generation fails — the orchestrator then falls back to the
     *                               offline template reasoner, so a decision never fails on the LLM.
     */
    String summarize(Submission submission,
                     DecisionOutcome outcome,
                     List<Finding> findings,
                     LearnedAssessment learned,
                     Money premium,
                     List<RetrievedSource> retrievedSources);
}
