package com.iqspark.underwriter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

    List<AuditEventEntity> findByReferenceOrderBySequenceAsc(String reference);
}
