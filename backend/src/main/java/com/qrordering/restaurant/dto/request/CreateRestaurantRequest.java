package com.qrordering.restaurant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new restaurant
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create restaurant request")
public class CreateRestaurantRequest {

    @NotBlank(message = "Restaurant name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Schema(description = "Restaurant name", example = "Golden Dragon Restaurant", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Restaurant description", example = "Authentic Chinese cuisine with over 20 years of tradition")
    private String description;

    @Size(max = 255, message = "Logo URL must not exceed 255 characters")
    @Schema(description = "Logo URL", example = "https://example.com/logo.png")
    private String logoUrl;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Schema(description = "Restaurant address", example = "123 Queen St, Auckland, New Zealand")
    private String address;

    @Pattern(regexp = "^\\+?[0-9\\s\\-()]+$", message = "Invalid phone number format")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Schema(description = "Contact phone number", example = "+64 9 123 4567")
    private String phone;
}
