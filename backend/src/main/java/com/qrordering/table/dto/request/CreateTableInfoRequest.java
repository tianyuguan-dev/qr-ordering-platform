package com.qrordering.table.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create table request")
public class CreateTableInfoRequest {

    @NotBlank(message = "Table number is required")
    @Size(max = 20)
    @Schema(description = "Table number/code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tableNumber;

    @Schema(description = "Number of seats")
    private Integer seats;
}
