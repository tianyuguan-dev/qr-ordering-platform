package com.qrordering.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrordering.event.dto.EventMessage;
import com.qrordering.event.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publish outbox event to Redis Pub/Sub. Channel: events:{eventType}.
 * TECH_DESIGN_V2 §4.4
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventPublisher implements EventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(OutboxEvent outboxEvent) {
        String channel = "events:" + outboxEvent.getEventType();
        EventMessage message = EventMessage.builder()
                .eventId(outboxEvent.getEventId())
                .eventType(outboxEvent.getEventType())
                .tenantId(outboxEvent.getTenantId())
                .payload(outboxEvent.getPayload())
                .timestamp(Instant.now())
                .build();
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel, json);
            log.debug("Published to Redis channel {}: {}", channel, message.getEventId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize EventMessage", e);
        }
    }
}
