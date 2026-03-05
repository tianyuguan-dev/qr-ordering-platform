package com.qrordering.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Checkout summary for a table (all active orders + combined total)")
public class TableCheckoutSummaryResponse {

    @Schema(description = "Table number (e.g. T01)")
    private String tableNumber;

    @Schema(description = "Table ID")
    private Long tableId;

    @Schema(description = "Number of active orders on this table")
    private int orderCount;

    @Schema(description = "Sum of totalAmount of all active orders")
    private BigDecimal tableTotal;

    @Schema(description = "Active orders (CREATED..SERVED)")
    private List<OrderResponse> orders;
}
