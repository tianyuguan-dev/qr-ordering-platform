package com.qrordering.menu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create category request")
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 50)
    @Schema(description = "Category name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Sort order (lower first)")
    private Integer sortOrder;
}
