package com.iqspark.underwriter.flywheel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RealizedOutcomeRepository extends JpaRepository<RealizedOutcomeEntity, Long> {

    long countByHadClaimTrue();

    @Query("select avg(o.lossRatio) from RealizedOutcomeEntity o")
    Double averageLossRatio();
}
