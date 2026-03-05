package com.qrordering.table.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Update table request")
public class UpdateTableInfoRequest {

    @Size(max = 20)
    @Schema(description = "Table number/code")
    private String tableNumber;

    @Schema(description = "Number of seats")
    private Integer seats;

    @Schema(description = "Status: 1=AVAILABLE, 2=OCCUPIED, 3=RESERVED")
    private Integer status;
}
