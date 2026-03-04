package com.qrordering.auth.dto.response;

import com.qrordering.common.dto.StatusInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for restaurant staff (no password).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Restaurant user (staff) information")
public class RestaurantUserResponse {

    @Schema(description = "User ID")
    private Long id;

    @Schema(description = "Restaurant (tenant) ID", example = "REST_20240304_ABC123")
    private String tenantId;

    @Schema(description = "Username", example = "waiter01")
    private String username;

    @Schema(description = "User role (code, name, description)")
    private StatusInfo role;

    @Schema(description = "Email", example = "waiter@example.com")
    private String email;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
