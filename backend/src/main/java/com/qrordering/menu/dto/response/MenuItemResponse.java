package com.qrordering.menu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Menu item response")
public class MenuItemResponse {

    @Schema(description = "Menu item ID")
    private Long id;

    @Schema(description = "Category ID")
    private Long categoryId;

    @Schema(description = "Item name")
    private String name;

    @Schema(description = "Item description")
    private String description;

    @Schema(description = "Unit price")
    private BigDecimal price;

    @Schema(description = "Image URL")
    private String imageUrl;

    @Schema(description = "Allergens info")
    private String allergens;

    @Schema(description = "Status code: 1=AVAILABLE, 2=SOLD_OUT, 3=INACTIVE")
    private Integer status;
}
