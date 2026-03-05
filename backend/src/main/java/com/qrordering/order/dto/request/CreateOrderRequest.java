package com.qrordering.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create order request (customer)")
public class CreateOrderRequest {

    @NotNull(message = "Table ID is required")
    @Schema(description = "Table ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tableId;

    @Valid
    @NotNull(message = "Items are required")
    @Size(min = 1, message = "At least one item is required")
    @Schema(description = "Order items", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrderItemRequest> items;

    @Size(max = 500)
    @Schema(description = "Customer notes")
    private String customerNotes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        @NotNull
        @Schema(description = "Menu item ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long menuItemId;

        @NotNull
        @Schema(description = "Quantity", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer quantity;
    }
}
