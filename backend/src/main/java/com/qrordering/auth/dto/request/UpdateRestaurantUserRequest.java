package com.qrordering.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Update restaurant staff (email, role)")
public class UpdateRestaurantUserRequest {

    @Size(max = 100)
    @Schema(description = "Email", example = "waiter@example.com")
    private String email;

    @Min(value = 1, message = "Role code must be 1, 2, or 3")
    @Max(value = 3, message = "Role code must be 1, 2, or 3")
    @Schema(description = "Role code (1=RESTAURANT_ADMIN, 2=WAITER, 3=KITCHEN)", example = "2")
    private Integer role;
}
