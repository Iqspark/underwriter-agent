package com.iqspark.underwriter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DecisionRepository extends JpaRepository<DecisionEntity, Long> {

    /** The latest stored decision for a reference. */
    Optional<DecisionEntity> findFirstByReferenceOrderByIdDesc(String reference);
}
