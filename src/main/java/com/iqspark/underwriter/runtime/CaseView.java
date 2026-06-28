package com.iqspark.underwriter.runtime;

import com.iqspark.underwriter.domain.decision.Decision;

import java.time.Instant;

/** API view of a case: its lifecycle state plus the decision once it has been processed. */
public record CaseView(
        String caseId,
        String reference,
        CaseStatus status,
        String outcome,
        String tier,
        String decisionReference,
        int attempts,
        Decision decision,
        Instant createdAt,
        Instant updatedAt
) {}
