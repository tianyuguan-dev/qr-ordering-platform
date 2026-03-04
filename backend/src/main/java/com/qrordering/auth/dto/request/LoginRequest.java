package com.qrordering.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for restaurant staff login.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Login request")
public class LoginRequest {

    @NotBlank(message = "Tenant ID (restaurant ID) is required")
    @Size(max = 50)
    @Schema(description = "Restaurant (tenant) ID", example = "REST_20240304_ABC123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantId;

    @NotBlank(message = "Username is required")
    @Size(max = 50)
    @Schema(description = "Username", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 128, message = "Password must not exceed 128 characters")
    @Schema(description = "Password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
