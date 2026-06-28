package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.audit.AuditTrail;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.RetrievedSource;
import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.enrichment.Enrichment;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import com.iqspark.underwriter.intake.SemanticFeatures;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared mutable state threaded through the agent pipeline: the submission, accumulating findings,
 * the learned assessment, the indicative premium, and the append-only audit trail.
 */
public class UnderwritingContext {

    private final Submission submission;
    private final List<Finding> findings = new ArrayList<>();
    private final AuditTrail auditTrail = new AuditTrail();
    private LearnedAssessment learnedAssessment;
    private Money indicativePremium;
    private List<RetrievedSource> retrievedSources = List.of();
    private Enrichment enrichment;
    private SemanticFeatures semanticFeatures;

    public UnderwritingContext(Submission submission) {
        this.submission = submission;
    }

    public Submission submission() {
        return submission;
    }

    public void addFindings(List<Finding> newFindings) {
        if (newFindings != null) {
            findings.addAll(newFindings);
        }
    }

    public void addFinding(Finding finding) {
        if (finding != null) {
            findings.add(finding);
        }
    }

    public List<Finding> findings() {
        return List.copyOf(findings);
    }

    public int riskScore() {
        return findings.stream().mapToInt(Finding::weight).sum();
    }

    /** Risk score excluding knockout weight — used for the pricing load so it isn't distorted. */
    public int pricingScore() {
        return findings.stream().filter(f -> !f.isKnockout()).mapToInt(Finding::weight).sum();
    }

    public boolean hasKnockout() {
        return findings.stream().anyMatch(Finding::isKnockout);
    }

    public LearnedAssessment learnedAssessment() {
        return learnedAssessment;
    }

    public void setLearnedAssessment(LearnedAssessment learnedAssessment) {
        this.learnedAssessment = learnedAssessment;
    }

    public Money indicativePremium() {
        return indicativePremium;
    }

    public void setIndicativePremium(Money indicativePremium) {
        this.indicativePremium = indicativePremium;
    }

    public List<RetrievedSource> retrievedSources() {
        return retrievedSources;
    }

    public void setRetrievedSources(List<RetrievedSource> retrievedSources) {
        this.retrievedSources = retrievedSources == null ? List.of() : List.copyOf(retrievedSources);
    }

    public Enrichment enrichment() {
        return enrichment;
    }

    public void setEnrichment(Enrichment enrichment) {
        this.enrichment = enrichment;
    }

    public SemanticFeatures semanticFeatures() {
        return semanticFeatures;
    }

    public void setSemanticFeatures(SemanticFeatures semanticFeatures) {
        this.semanticFeatures = semanticFeatures;
    }

    public void audit(String agent, String detail) {
        auditTrail.record(agent, detail);
    }

    public AuditTrail auditTrail() {
        return auditTrail;
    }
}
