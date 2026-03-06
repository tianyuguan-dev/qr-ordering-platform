package com.qrordering.event.repository;

import com.qrordering.event.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** Find NEW events ready to process (next_retry_at null or in the past). */
    List<OutboxEvent> findByStatusAndNextRetryAtBeforeOrNextRetryAtIsNullOrderByIdAsc(
            Integer status, Instant before, Pageable pageable);

    /** Count events by status (for metrics and health). */
    long countByStatus(Integer status);
}
