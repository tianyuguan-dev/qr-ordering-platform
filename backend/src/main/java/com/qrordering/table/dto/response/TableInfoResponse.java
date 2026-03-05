package com.qrordering.table.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Table info response")
public class TableInfoResponse {

    @Schema(description = "Table ID")
    private Long id;

    @Schema(description = "Table number/code")
    private String tableNumber;

    @Schema(description = "QR code URL")
    private String qrCodeUrl;

    @Schema(description = "Seats")
    private Integer seats;

    @Schema(description = "Status code: 1=AVAILABLE, 2=OCCUPIED, 3=RESERVED")
    private Integer status;
}
