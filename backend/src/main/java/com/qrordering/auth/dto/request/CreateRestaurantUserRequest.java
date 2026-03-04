package com.qrordering.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a restaurant staff user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create restaurant user request")
public class CreateRestaurantUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 1, max = 50)
    @Schema(description = "Username (unique within this restaurant)", example = "waiter01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
    @Schema(description = "Password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotNull(message = "Role is required")
    @Min(value = 1, message = "Role code must be 1, 2, or 3")
    @Max(value = 3, message = "Role code must be 1, 2, or 3")
    @Schema(description = "Role code (1=RESTAURANT_ADMIN, 2=WAITER, 3=KITCHEN)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer role;

    @Size(max = 100)
    @Schema(description = "Email", example = "waiter@example.com")
    private String email;
}
