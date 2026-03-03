package com.qrordering.restaurant.dto.response;

import com.qrordering.common.dto.StatusInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Restaurant response DTO for API response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Restaurant information")
public class RestaurantResponse {

    @Schema(description = "Restaurant ID", example = "REST_20240304_00001")
    private String id;

    @Schema(description = "Restaurant name", example = "Golden Dragon Restaurant")
    private String name;

    @Schema(description = "Restaurant description", example = "Authentic Chinese cuisine")
    private String description;

    @Schema(description = "Logo URL", example = "https://example.com/logo.png")
    private String logoUrl;

    @Schema(description = "Restaurant address", example = "123 Queen St, Auckland")
    private String address;

    @Schema(description = "Contact phone", example = "+64 9 123 4567")
    private String phone;

    @Schema(description = "Restaurant status information")
    private StatusInfo status;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Last update time")
    private LocalDateTime updatedAt;
}
