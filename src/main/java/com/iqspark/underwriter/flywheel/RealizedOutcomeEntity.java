package com.iqspark.underwriter.flywheel;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A realized outcome for a past decision (claim / no claim, loss ratio) — the flywheel input that
 * lets us compare predicted vs realized and (later) refresh the book, RAG corpus and eval set.
 */
@Entity
@Table(name = "realized_outcome",
        indexes = @Index(name = "idx_outcome_reference", columnList = "reference"))
public class RealizedOutcomeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;
    private boolean hadClaim;
    private double lossRatio;
    private Instant recordedAt;

    protected RealizedOutcomeEntity() {
    }

    public RealizedOutcomeEntity(String reference, boolean hadClaim, double lossRatio) {
        this.reference = reference;
        this.hadClaim = hadClaim;
        this.lossRatio = lossRatio;
        this.recordedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public boolean isHadClaim() { return hadClaim; }
    public double getLossRatio() { return lossRatio; }
    public Instant getRecordedAt() { return recordedAt; }
}
