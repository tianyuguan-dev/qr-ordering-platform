package com.qrordering.restaurant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating restaurant information
 *
 * This is a partial update - only non-null fields will be updated.
 * All fields are optional.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Update restaurant request")
public class UpdateRestaurantRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Schema(description = "Restaurant name", example = "Golden Dragon Restaurant")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Restaurant description", example = "Authentic Chinese cuisine")
    private String description;

    @Size(max = 255, message = "Logo URL must not exceed 255 characters")
    @Schema(description = "Logo URL", example = "https://example.com/logo.png")
    private String logoUrl;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Schema(description = "Restaurant address", example = "123 Queen St, Auckland")
    private String address;

    @Pattern(regexp = "^\\+?[0-9\\s\\-()]+$", message = "Invalid phone number format")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Schema(description = "Contact phone number", example = "+64 9 123 4567")
    private String phone;

    @Min(value = 1, message = "Status code must be between 1 and 3")
    @Max(value = 3, message = "Status code must be between 1 and 3")
    @Schema(description = "Restaurant status code (1=ACTIVE, 2=SUSPENDED, 3=INACTIVE)", example = "1")
    private Integer status;
}
