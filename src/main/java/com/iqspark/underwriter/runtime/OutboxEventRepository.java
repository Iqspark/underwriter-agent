package com.iqspark.underwriter.runtime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findByPublishedFalseOrderByIdAsc();
}
