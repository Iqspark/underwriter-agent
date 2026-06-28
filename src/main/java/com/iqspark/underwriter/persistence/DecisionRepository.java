package com.iqspark.underwriter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DecisionRepository extends JpaRepository<DecisionEntity, Long> {

    /** The latest stored decision for a reference. */
    Optional<DecisionEntity> findFirstByReferenceOrderByIdDesc(String reference);

    long countByOutcome(String outcome);

    long countByTier(String tier);

    @Query("select avg(d.premiumAmount) from DecisionEntity d")
    Double averagePremium();

    @Query("select avg(d.predictedClaimProbability) from DecisionEntity d where d.predictedClaimProbability is not null")
    Double averagePredictedClaimProbability();
}
