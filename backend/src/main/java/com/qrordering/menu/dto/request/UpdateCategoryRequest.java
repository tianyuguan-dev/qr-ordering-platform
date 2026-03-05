package com.qrordering.menu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Update category request")
public class UpdateCategoryRequest {

    @Size(max = 50)
    @Schema(description = "Category name")
    private String name;

    @Schema(description = "Sort order")
    private Integer sortOrder;
}
