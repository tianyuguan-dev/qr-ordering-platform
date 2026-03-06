package com.qrordering.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusChangedEventPayload {

    private String eventId;
    private String tenantId;
    private Long orderId;
    private Integer oldStatus;
    private Integer newStatus;
    private Instant occurredAt;
}
