package com.qrordering.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Order response")
public class OrderResponse {

    @Schema(description = "Order ID")
    private Long id;

    @Schema(description = "Order number")
    private String orderNumber;

    @Schema(description = "Table ID")
    private Long tableId;

    @Schema(description = "Status code")
    private Integer status;

    @Schema(description = "Total amount")
    private BigDecimal totalAmount;

    @Schema(description = "Customer notes")
    private String customerNotes;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @Schema(description = "Order items")
    private List<OrderItemResponse> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long menuItemId;
        private String name;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
