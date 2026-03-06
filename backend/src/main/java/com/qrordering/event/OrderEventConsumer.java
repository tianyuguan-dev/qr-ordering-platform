package com.qrordering.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrordering.event.dto.EventMessage;
import com.qrordering.event.dto.OrderCreatedEventPayload;
import com.qrordering.event.dto.OrderStatusChangedEventPayload;
import com.qrordering.event.entity.ProcessedEvent;
import com.qrordering.event.repository.ProcessedEventRepository;
import com.qrordering.sse.SseMessage;
import com.qrordering.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Consume order events from Redis and push via SSE. TECH_DESIGN_V2 §5.2
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private static final String PROCESSED_KEY_PREFIX = "processed_event:";
    private static final Duration PROCESSED_TTL = Duration.ofDays(7);

    private final SseEmitterManager sseManager;
    private final ProcessedEventRepository processedEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void onOrderCreated(String messageJson) {
        handleMessage(messageJson, "OrderCreatedEventPayload", (msg, payload) -> {
            OrderCreatedEventPayload event = objectMapper.readValue(msg.getPayload(), OrderCreatedEventPayload.class);
            sseManager.broadcast(event.getTenantId(), SseMessage.builder()
                    .type("ORDER_CREATED")
                    .data(event)
                    .build());
        });
    }

    public void onOrderStatusChanged(String messageJson) {
        handleMessage(messageJson, "OrderStatusChangedEventPayload", (msg, payload) -> {
            OrderStatusChangedEventPayload event = objectMapper.readValue(msg.getPayload(), OrderStatusChangedEventPayload.class);
            sseManager.broadcast(event.getTenantId(), SseMessage.builder()
                    .type("ORDER_STATUS_CHANGED")
                    .data(event)
                    .build());
        });
    }

    private void handleMessage(String messageJson, String payloadType, MessageHandler handler) {
        try {
            EventMessage msg = objectMapper.readValue(messageJson, EventMessage.class);
            if (isProcessed(msg.getEventId())) {
                log.debug("Event {} already processed", msg.getEventId());
                return;
            }
            handler.handle(msg, msg.getPayload());
            markProcessed(msg.getEventId());
        } catch (Exception e) {
            log.error("Failed to process {}: {}", payloadType, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean isProcessed(String eventId) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(PROCESSED_KEY_PREFIX + eventId))) {
            return true;
        }
        return processedEventRepository.existsByEventId(eventId);
    }

    @Transactional
    public void markProcessed(String eventId) {
        try {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .processedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.debug("Event {} already marked processed (duplicate)", eventId);
        }
        redisTemplate.opsForValue().set(PROCESSED_KEY_PREFIX + eventId, "1", PROCESSED_TTL);
    }

    @FunctionalInterface
    private interface MessageHandler {
        void handle(EventMessage msg, String payload) throws Exception;
    }
}
