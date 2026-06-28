package com.iqspark.underwriter.runtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/** Durable per-submission case: lifecycle state, idempotency key, and the stored submission/decision. */
@Entity
@Table(name = "submission_case",
        indexes = @Index(name = "idx_case_idempotency", columnList = "idempotencyKey"))
public class SubmissionCaseEntity {

    @Id
    private String caseId;             // also the correlation id

    private String idempotencyKey;
    private String reference;
    private String line;

    @Enumerated(EnumType.STRING)
    private CaseStatus status;

    private String outcome;
    private String tier;
    private String decisionReference;
    private int attempts;
    private String lastError;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String submissionJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String decisionJson;

    private Instant createdAt;
    private Instant updatedAt;

    protected SubmissionCaseEntity() {
    }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getLine() { return line; }
    public void setLine(String line) { this.line = line; }
    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
    public String getDecisionReference() { return decisionReference; }
    public void setDecisionReference(String decisionReference) { this.decisionReference = decisionReference; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public String getSubmissionJson() { return submissionJson; }
    public void setSubmissionJson(String submissionJson) { this.submissionJson = submissionJson; }
    public String getDecisionJson() { return decisionJson; }
    public void setDecisionJson(String decisionJson) { this.decisionJson = decisionJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
