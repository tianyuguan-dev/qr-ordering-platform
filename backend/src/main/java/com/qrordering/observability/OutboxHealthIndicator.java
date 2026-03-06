package com.qrordering.observability;

import com.qrordering.event.entity.OutboxEvent;
import com.qrordering.event.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health check for outbox: backlog and dead-letter count. TECH_DESIGN_V2 §7.3
 */
@Component
@RequiredArgsConstructor
public class OutboxHealthIndicator implements HealthIndicator {

    private static final long DEAD_THRESHOLD = 100;
    private static final long BACKLOG_DEGRADED_THRESHOLD = 10_000;

    private final OutboxEventRepository outboxEventRepository;

    @Override
    public Health health() {
        long backlog = outboxEventRepository.countByStatus(OutboxEvent.STATUS_NEW);
        long dead = outboxEventRepository.countByStatus(OutboxEvent.STATUS_DEAD);

        if (dead > DEAD_THRESHOLD) {
            return Health.down()
                    .withDetail("backlog", backlog)
                    .withDetail("deadLetters", dead)
                    .withDetail("message", "Too many dead-letter events in outbox")
                    .build();
        }

        if (backlog > BACKLOG_DEGRADED_THRESHOLD) {
            return Health.up()
                    .withDetail("backlog", backlog)
                    .withDetail("deadLetters", dead)
                    .withDetail("highBacklog", true)
                    .withDetail("message", "High outbox backlog")
                    .build();
        }

        return Health.up()
                .withDetail("backlog", backlog)
                .withDetail("deadLetters", dead)
                .build();
    }
}
