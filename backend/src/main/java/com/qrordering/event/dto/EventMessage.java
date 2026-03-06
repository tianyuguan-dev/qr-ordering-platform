package com.qrordering.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Message envelope for Redis Pub/Sub (eventId, type, tenantId, payload JSON, timestamp). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventMessage {

    private String eventId;
    private String eventType;
    private String tenantId;
    private String payload;
    private Instant timestamp;
}
