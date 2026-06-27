package com.iqspark.underwriter.persistence;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.history.model.LearnedAssessment;

import java.time.Instant;
import java.util.List;

/** A persisted decision plus its durable, hash-chained audit lineage and chain-validity flag. */
public record StoredDecision(
        String reference,
        String line,
        String outcome,
        int riskScore,
        double coverageAmount,
        Money indicativePremium,
        String rationale,
        List<Finding> findings,
        List<String> conditions,
        LearnedAssessment learnedAssessment,
        Instant decidedAt,
        List<AuditLine> auditTrail,
        boolean auditChainValid
) {
    public record AuditLine(int sequence, String detail, String prevHash, String hash) {}
}
