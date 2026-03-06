package com.qrordering.event;

import com.qrordering.event.entity.OutboxEvent;

/**
 * Publish outbox event to Redis (or MQ). Used by OutboxProcessor.
 * TECH_DESIGN_V2 §4.4
 */
public interface EventPublisher {

    void publish(OutboxEvent outboxEvent);
}
