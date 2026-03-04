package com.qrordering.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to change current user's password.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Change password request")
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Schema(description = "Current password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 128, message = "New password must be between 6 and 128 characters")
    @Schema(description = "New password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
