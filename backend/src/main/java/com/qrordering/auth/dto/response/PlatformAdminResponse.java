package com.qrordering.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for platform admin (no password).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Platform admin (no password)")
public class PlatformAdminResponse {

    @Schema(description = "Admin ID")
    private Long id;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;
}
