package com.qrordering.event.repository;

import com.qrordering.event.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** Find NEW events ready to process (next_retry_at null or in the past). */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :before) ORDER BY e.id ASC")
    List<OutboxEvent> findReadyEvents(@Param("status") Integer status, @Param("before") Instant before, Pageable pageable);

    /** Count events by status (for metrics and health). */
    long countByStatus(Integer status);
}
