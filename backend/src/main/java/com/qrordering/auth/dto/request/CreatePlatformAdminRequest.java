package com.qrordering.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a platform administrator.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create platform admin request")
public class CreatePlatformAdminRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 1, max = 50)
    @Schema(description = "Username (unique)", example = "admin2", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
    @Schema(description = "Password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Size(max = 100)
    @Schema(description = "Email", example = "admin@platform.com")
    private String email;
}
