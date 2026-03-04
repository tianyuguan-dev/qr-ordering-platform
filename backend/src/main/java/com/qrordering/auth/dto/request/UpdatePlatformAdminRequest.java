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
@Schema(description = "Update platform admin (email only)")
public class UpdatePlatformAdminRequest {

    @Size(max = 100)
    @Schema(description = "Email", example = "admin@example.com")
    private String email;
}
