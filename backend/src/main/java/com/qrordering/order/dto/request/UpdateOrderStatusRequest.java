package com.qrordering.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Update order status request")
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Status code: 1=CREATED, 2=CONFIRMED, 3=PREPARING, 4=READY, 5=SERVED, 6=COMPLETED, 7=CANCELLED", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
