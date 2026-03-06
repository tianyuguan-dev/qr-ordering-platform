package com.qrordering.observability;

import com.qrordering.event.entity.OutboxEvent;
import com.qrordering.event.repository.OutboxEventRepository;
import com.qrordering.order.enums.OrderStatus;
import com.qrordering.sse.SseEmitterManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Custom business metrics for observability. TECH_DESIGN_V2 §7.1
 */
@Component
@Slf4j
public class MetricsService {

    private final Counter orderCreatedCounter;
    private final Counter orderStatusChangedCounter;
    private final Counter outboxPublishFailedCounter;
    private final OutboxEventRepository outboxEventRepository;
    private final SseEmitterManager sseEmitterManager;

    public MetricsService(MeterRegistry registry,
                          OutboxEventRepository outboxEventRepository,
                          SseEmitterManager sseEmitterManager) {
        this.outboxEventRepository = outboxEventRepository;
        this.sseEmitterManager = sseEmitterManager;

        this.orderCreatedCounter = Counter.builder("orders.created")
                .description("Total orders created")
                .tag("app", "qr-ordering")
                .register(registry);

        this.orderStatusChangedCounter = Counter.builder("orders.status.changed")
                .description("Total order status changes")
                .tag("app", "qr-ordering")
                .register(registry);

        this.outboxPublishFailedCounter = Counter.builder("outbox.publish.failed")
                .description("Failed outbox event publications")
                .tag("app", "qr-ordering")
                .register(registry);

        Gauge.builder("outbox.backlog", this, MetricsService::getOutboxBacklog)
                .description("Number of unprocessed events in outbox (NEW status)")
                .register(registry);

        Gauge.builder("outbox.dead", this, MetricsService::getOutboxDead)
                .description("Number of dead-letter events in outbox")
                .register(registry);

        Gauge.builder("sse.connections", this, MetricsService::getSseConnections)
                .description("Active SSE connections across all tenants")
                .register(registry);
    }

    public void recordOrderCreated(String tenantId) {
        orderCreatedCounter.increment();
    }

    public void recordOrderStatusChanged(String tenantId, OrderStatus from, OrderStatus to) {
        orderStatusChangedCounter.increment();
    }

    public void recordOutboxPublishFailed() {
        outboxPublishFailedCounter.increment();
    }

    private double getOutboxBacklog() {
        try {
            return outboxEventRepository.countByStatus(OutboxEvent.STATUS_NEW);
        } catch (Exception e) {
            log.debug("Failed to get outbox backlog", e);
            return -1;
        }
    }

    private double getOutboxDead() {
        try {
            return outboxEventRepository.countByStatus(OutboxEvent.STATUS_DEAD);
        } catch (Exception e) {
            log.debug("Failed to get outbox dead count", e);
            return -1;
        }
    }

    private double getSseConnections() {
        return sseEmitterManager.getTotalConnections();
    }
}
