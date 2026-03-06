package com.qrordering.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEventPayload {

    private String eventId;
    private String tenantId;
    private Long orderId;
    private String orderNumber;
    private Long tableId;
    private String tableNumber;
    private BigDecimal totalAmount;
    private List<OrderItemDto> items;
    private Instant occurredAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto {
        private Long menuItemId;
        private String name;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
