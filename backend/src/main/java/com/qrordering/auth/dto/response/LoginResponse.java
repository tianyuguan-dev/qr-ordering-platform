package com.qrordering.auth.dto.response;

import com.qrordering.common.dto.StatusInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for successful login.
 * accessToken reserved for JWT in a later step.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Login response")
public class LoginResponse {

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Restaurant (tenant) ID", example = "REST_20240304_ABC123")
    private String tenantId;

    @Schema(description = "Username", example = "admin")
    private String username;

    @Schema(description = "User role (code, name, description)")
    private StatusInfo role;

    @Schema(description = "JWT access token (placeholder, will be populated when JWT is implemented)")
    private String accessToken;
}
