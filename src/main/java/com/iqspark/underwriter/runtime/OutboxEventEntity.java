package com.iqspark.underwriter.runtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Outbox row written atomically with a state change, then relayed (in-process today; a Kafka topic
 * later) — the outbox pattern, so a DB write and an event publish never diverge.
 */
@Entity
@Table(name = "outbox_event",
        indexes = @Index(name = "idx_outbox_published", columnList = "published"))
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String caseId;
    private String type;          // SubmissionReceived | DecisionMade | DecisionFailed

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    private boolean published;
    private Instant createdAt;

    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(String caseId, String type, String payload) {
        this.caseId = caseId;
        this.type = type;
        this.payload = payload;
        this.published = false;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getCaseId() { return caseId; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public Instant getCreatedAt() { return createdAt; }
}
