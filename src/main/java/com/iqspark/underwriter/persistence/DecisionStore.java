package com.iqspark.underwriter.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqspark.underwriter.domain.decision.Decision;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persists decisions and a tamper-evident, hash-chained audit trail, and reads them back. A
 * persistence failure is surfaced to the caller (the orchestrator catches it so a decision is never
 * failed by a store outage — degrade-to-floor).
 */
@Service
public class DecisionStore {

    private final DecisionRepository decisions;
    private final AuditEventRepository auditEvents;
    private final ObjectMapper mapper;

    public DecisionStore(DecisionRepository decisions, AuditEventRepository auditEvents, ObjectMapper mapper) {
        this.decisions = decisions;
        this.auditEvents = auditEvents;
        this.mapper = mapper;
    }

    @Transactional
    public void save(Decision decision, String line, double coverageAmount) {
        DecisionEntity e = new DecisionEntity();
        e.setReference(decision.reference());
        e.setLine(line);
        e.setOutcome(decision.outcome().name());
        e.setRiskScore(decision.riskScore());
        e.setCoverageAmount(coverageAmount);
        if (decision.indicativePremium() != null) {
            e.setPremiumAmount(decision.indicativePremium().amount());
            e.setPremiumCurrency(decision.indicativePremium().currency());
        }
        if (decision.autonomy() != null) {
            e.setTier(decision.autonomy().tier().name());
        }
        if (decision.learnedAssessment() != null) {
            e.setPredictedClaimProbability(decision.learnedAssessment().claimProbability());
            e.setExpectedLossRatio(decision.learnedAssessment().expectedLossRatio());
        }
        e.setRationale(decision.rationale());
        e.setFindingsJson(toJson(decision.findings()));
        e.setConditionsJson(toJson(decision.conditions()));
        e.setLearnedJson(toJson(decision.learnedAssessment()));
        e.setDecidedAt(decision.decidedAt());
        decisions.save(e);

        String reference = decision.reference() == null ? "(no-ref)" : decision.reference();
        String prevHash = "";
        int seq = 0;
        for (String detail : decision.auditTrail()) {
            String hash = sha256(prevHash + seq + detail);
            auditEvents.save(new AuditEventEntity(reference, seq, detail, prevHash, hash, Instant.now()));
            prevHash = hash;
            seq++;
        }
    }

    @Transactional(readOnly = true)
    public Optional<StoredDecision> find(String reference) {
        return decisions.findFirstByReferenceOrderByIdDesc(reference).map(e -> {
            List<AuditEventEntity> events = auditEvents.findByReferenceOrderBySequenceAsc(reference);
            List<StoredDecision.AuditLine> lines = new ArrayList<>();
            for (AuditEventEntity ev : events) {
                lines.add(new StoredDecision.AuditLine(ev.getSequence(), ev.getDetail(), ev.getPrevHash(), ev.getHash()));
            }
            Money premium = e.getPremiumCurrency() == null ? null
                    : new Money(e.getPremiumAmount(), e.getPremiumCurrency());
            return new StoredDecision(
                    e.getReference(), e.getLine(), e.getOutcome(), e.getRiskScore(),
                    e.getCoverageAmount(), premium,
                    e.getRationale(),
                    fromJson(e.getFindingsJson(), new TypeReference<List<Finding>>() {}),
                    fromJson(e.getConditionsJson(), new TypeReference<List<String>>() {}),
                    fromJsonObject(e.getLearnedJson(), LearnedAssessment.class),
                    e.getDecidedAt(),
                    lines,
                    verifyAuditChain(reference));
        });
    }

    /** Recompute the hash chain for a reference and confirm no entry was edited after the fact. */
    @Transactional(readOnly = true)
    public boolean verifyAuditChain(String reference) {
        List<AuditEventEntity> events = auditEvents.findByReferenceOrderBySequenceAsc(reference);
        String prevHash = "";
        for (AuditEventEntity ev : events) {
            String expected = sha256(prevHash + ev.getSequence() + ev.getDetail());
            if (!expected.equals(ev.getHash()) || !prevHash.equals(ev.getPrevHash())) {
                return false;
            }
            prevHash = ev.getHash();
        }
        return true;
    }

    private String toJson(Object value) {
        try {
            return value == null ? null : mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize decision part", e);
        }
    }

    private <T> List<T> fromJson(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize decision part", e);
        }
    }

    private <T> T fromJsonObject(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize decision part", e);
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
