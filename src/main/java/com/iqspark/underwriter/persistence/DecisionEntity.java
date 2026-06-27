package com.iqspark.underwriter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/** Persistent record of a decision. Variable-shape parts are stored as JSON text for schema stability. */
@Entity
@Table(name = "decision", indexes = @Index(name = "idx_decision_reference", columnList = "reference"))
public class DecisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;
    private String line;
    private String outcome;
    private int riskScore;
    private double coverageAmount;
    private double premiumAmount;
    private String premiumCurrency;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String findingsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String conditionsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String learnedJson;

    private Instant decidedAt;

    protected DecisionEntity() {
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getLine() { return line; }
    public void setLine(String line) { this.line = line; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public double getCoverageAmount() { return coverageAmount; }
    public void setCoverageAmount(double coverageAmount) { this.coverageAmount = coverageAmount; }
    public double getPremiumAmount() { return premiumAmount; }
    public void setPremiumAmount(double premiumAmount) { this.premiumAmount = premiumAmount; }
    public String getPremiumCurrency() { return premiumCurrency; }
    public void setPremiumCurrency(String premiumCurrency) { this.premiumCurrency = premiumCurrency; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public String getFindingsJson() { return findingsJson; }
    public void setFindingsJson(String findingsJson) { this.findingsJson = findingsJson; }
    public String getConditionsJson() { return conditionsJson; }
    public void setConditionsJson(String conditionsJson) { this.conditionsJson = conditionsJson; }
    public String getLearnedJson() { return learnedJson; }
    public void setLearnedJson(String learnedJson) { this.learnedJson = learnedJson; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}
