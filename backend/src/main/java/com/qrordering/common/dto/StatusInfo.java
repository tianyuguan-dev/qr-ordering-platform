package com.qrordering.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Status information DTO
 *
 * Generic DTO for representing enum status information.
 * Can be reused across different modules (Restaurant, Order, MenuItem, etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Status information")
public class StatusInfo {

    @Schema(description = "Status code", example = "1")
    private Integer code;

    @Schema(description = "Status name", example = "ACTIVE")
    private String name;

    @Schema(description = "Status description", example = "Active")
    private String description;
}
