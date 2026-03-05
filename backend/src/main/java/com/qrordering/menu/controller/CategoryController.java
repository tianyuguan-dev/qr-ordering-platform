package com.qrordering.menu.controller;

import com.qrordering.menu.dto.request.CreateCategoryRequest;
import com.qrordering.menu.dto.request.UpdateCategoryRequest;
import com.qrordering.menu.dto.response.CategoryResponse;
import com.qrordering.menu.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants/{restaurantId}/categories")
@RequiredArgsConstructor
@Tag(name = "Menu Categories", description = "APIs for managing menu categories. Use restaurantId=me for current restaurant (restaurant admin), or restaurant id (platform admin).")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List categories")
    public ResponseEntity<List<CategoryResponse>> list(@PathVariable String restaurantId) {
        return ResponseEntity.ok(categoryService.listForRestaurant(restaurantId));
    }

    @PostMapping
    @Operation(summary = "Create category")
    public ResponseEntity<CategoryResponse> create(@PathVariable String restaurantId,
                                                   @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(restaurantId, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category")
    public ResponseEntity<CategoryResponse> update(@PathVariable String restaurantId,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(restaurantId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category")
    public ResponseEntity<Void> delete(@PathVariable String restaurantId, @PathVariable Long id) {
        categoryService.delete(restaurantId, id);
        return ResponseEntity.noContent().build();
    }
}
