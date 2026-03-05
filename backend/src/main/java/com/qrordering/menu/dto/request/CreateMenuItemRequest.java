package com.qrordering.menu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create menu item request")
public class CreateMenuItemRequest {

    @Schema(description = "Category ID (optional)")
    private Long categoryId;

    @NotBlank(message = "Item name is required")
    @Size(max = 100)
    @Schema(description = "Item name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 2000)
    @Schema(description = "Item description")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0", inclusive = true)
    @Schema(description = "Unit price", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @Size(max = 255)
    @Schema(description = "Image URL")
    private String imageUrl;

    @Size(max = 255)
    @Schema(description = "Allergens info")
    private String allergens;
}
