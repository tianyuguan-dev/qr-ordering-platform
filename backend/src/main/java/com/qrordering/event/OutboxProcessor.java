package com.qrordering.event;

import com.qrordering.event.entity.OutboxEvent;
import com.qrordering.event.repository.OutboxEventRepository;
import com.qrordering.observability.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Poll outbox and publish events to Redis. TECH_DESIGN_V2 §4.3
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final EventPublisher eventPublisher;
    private final MetricsService metricsService;

    @Scheduled(fixedDelay = 10000) // 10 seconds
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxEventRepository
                .findByStatusAndNextRetryAtBeforeOrNextRetryAtIsNullOrderByIdAsc(
                        OutboxEvent.STATUS_NEW,
                        Instant.now(),
                        PageRequest.of(0, BATCH_SIZE));

        if (events.isEmpty()) {
            return;
        }
        log.debug("Processing {} outbox events", events.size());

        for (OutboxEvent event : events) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        try {
            event.setStatus(OutboxEvent.STATUS_PROCESSING);
            outboxEventRepository.save(event);

            eventPublisher.publish(event);

            event.setStatus(OutboxEvent.STATUS_SENT);
            event.setProcessedAt(Instant.now());
            outboxEventRepository.save(event);
            log.debug("Sent outbox event: {}", event.getEventId());
        } catch (Exception e) {
            handleFailure(event, e);
        }
    }

    private void handleFailure(OutboxEvent event, Exception e) {
        metricsService.recordOutboxPublishFailed();
        event.setAttemptCount(event.getAttemptCount() + 1);
        event.setErrorMessage(e.getMessage());
        if (event.getAttemptCount() >= MAX_ATTEMPTS) {
            event.setStatus(OutboxEvent.STATUS_DEAD);
            log.error("Event {} moved to DEAD after {} attempts", event.getEventId(), MAX_ATTEMPTS);
        } else {
            event.setStatus(OutboxEvent.STATUS_NEW);
            long delaySeconds = (long) Math.pow(2, event.getAttemptCount());
            event.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
            log.warn("Event {} failed, will retry: {}", event.getEventId(), e.getMessage());
        }
        outboxEventRepository.save(event);
    }
}
