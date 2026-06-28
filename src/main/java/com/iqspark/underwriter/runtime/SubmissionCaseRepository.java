package com.iqspark.underwriter.runtime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubmissionCaseRepository extends JpaRepository<SubmissionCaseEntity, String> {

    /** Most recent case for an idempotency key (used to dedupe re-submissions). */
    Optional<SubmissionCaseEntity> findFirstByIdempotencyKeyOrderByCreatedAtDesc(String idempotencyKey);
}
