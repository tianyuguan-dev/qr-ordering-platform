package com.qrordering.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrordering.event.entity.OutboxEvent;
import com.qrordering.event.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Publish domain events to outbox (same transaction as business write).
 * TECH_DESIGN_V2 §4.2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void publish(String eventType, String tenantId, Long aggregateId, Object payload) {
        String eventId = UUID.randomUUID().toString();
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload", e);
        }
        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .tenantId(tenantId)
                .aggregateId(aggregateId)
                .payload(payloadJson)
                .status(OutboxEvent.STATUS_NEW)
                .attemptCount(0)
                .createdAt(Instant.now())
                .build();
        outboxEventRepository.save(event);
        log.debug("Published to outbox: eventId={}, type={}", eventId, eventType);
    }
}
