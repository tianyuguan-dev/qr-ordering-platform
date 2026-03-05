package com.qrordering.menu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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
@Schema(description = "Update menu item request")
public class UpdateMenuItemRequest {

    @Schema(description = "Category ID")
    private Long categoryId;

    @Size(max = 100)
    @Schema(description = "Item name")
    private String name;

    @Size(max = 2000)
    @Schema(description = "Item description")
    private String description;

    @DecimalMin(value = "0", inclusive = true)
    @Schema(description = "Unit price")
    private BigDecimal price;

    @Size(max = 255)
    @Schema(description = "Image URL")
    private String imageUrl;

    @Size(max = 255)
    @Schema(description = "Allergens info")
    private String allergens;

    @Schema(description = "Status: 1=AVAILABLE, 2=SOLD_OUT, 3=INACTIVE")
    private Integer status;
}
