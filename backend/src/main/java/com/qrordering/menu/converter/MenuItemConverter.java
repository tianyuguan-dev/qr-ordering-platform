package com.qrordering.menu.converter;

import com.qrordering.menu.dto.response.MenuItemResponse;
import com.qrordering.menu.entity.MenuItem;
import org.springframework.stereotype.Component;

@Component
public class MenuItemConverter {

    public MenuItemResponse toResponse(MenuItem entity) {
        if (entity == null) return null;
        return MenuItemResponse.builder()
                .id(entity.getId())
                .categoryId(entity.getCategoryId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .imageUrl(entity.getImageUrl())
                .allergens(entity.getAllergens())
                .status(entity.getStatus() != null ? entity.getStatus().getCode() : null)
                .build();
    }
}
