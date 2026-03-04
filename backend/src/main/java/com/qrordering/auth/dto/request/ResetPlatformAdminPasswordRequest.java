package com.qrordering.auth.dto.request;

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
@Schema(description = "Reset platform admin password (by another admin)")
public class ResetPlatformAdminPasswordRequest {

    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "New password", example = "newSecurePass123")
    private String newPassword;
}
