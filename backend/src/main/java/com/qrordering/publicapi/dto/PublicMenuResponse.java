package com.qrordering.publicapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicMenuResponse {

    private List<CategoryDto> categories;
    private List<MenuItemDto> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDto {
        private Long id;
        private String name;
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuItemDto {
        private Long id;
        private Long categoryId;
        private String name;
        private String description;
        private BigDecimal price;
        private String imageUrl;
        private String allergens;
    }
}
