package com.qrordering.menu.converter;

import com.qrordering.menu.dto.response.CategoryResponse;
import com.qrordering.menu.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryConverter {

    public CategoryResponse toResponse(Category entity) {
        if (entity == null) return null;
        return CategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}
