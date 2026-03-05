package com.qrordering.menu.dto.request;

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
@Schema(description = "Update menu item status only (e.g. for kitchen: sold out / inactive)")
public class UpdateMenuItemStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Status: 1=AVAILABLE, 2=SOLD_OUT, 3=INACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
