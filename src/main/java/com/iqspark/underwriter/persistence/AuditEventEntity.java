package com.iqspark.underwriter.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/** One append-only, hash-chained audit entry for a decision (tamper-evidence). */
@Entity
@Table(name = "audit_event", indexes = @Index(name = "idx_audit_reference", columnList = "reference"))
public class AuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;
    private int sequence;
    private String detail;
    private String prevHash;
    private String hash;
    private Instant at;

    protected AuditEventEntity() {
    }

    public AuditEventEntity(String reference, int sequence, String detail,
                            String prevHash, String hash, Instant at) {
        this.reference = reference;
        this.sequence = sequence;
        this.detail = detail;
        this.prevHash = prevHash;
        this.hash = hash;
        this.at = at;
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public int getSequence() { return sequence; }
    public String getDetail() { return detail; }
    public String getPrevHash() { return prevHash; }
    public String getHash() { return hash; }
    public Instant getAt() { return at; }
}
