package com.qrordering.menu.controller;

import com.qrordering.menu.dto.request.CreateMenuItemRequest;
import com.qrordering.menu.dto.request.UpdateMenuItemRequest;
import com.qrordering.menu.dto.response.MenuItemResponse;
import com.qrordering.menu.service.MenuItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants/{restaurantId}/menu-items")
@RequiredArgsConstructor
@Tag(name = "Menu Items", description = "APIs for managing menu items. Use restaurantId=me (restaurant admin) or restaurant id (platform admin).")
public class MenuItemController {

    private final MenuItemService menuItemService;

    @GetMapping("/all")
    @Operation(summary = "List all items (no paging)")
    public ResponseEntity<List<MenuItemResponse>> listAll(@PathVariable String restaurantId) {
        return ResponseEntity.ok(menuItemService.listAllForRestaurant(restaurantId));
    }

    @GetMapping
    @Operation(summary = "List items (paged)")
    public ResponseEntity<Page<MenuItemResponse>> list(
            @PathVariable String restaurantId,
            @Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by status: 1=AVAILABLE, 2=SOLD_OUT, 3=INACTIVE") @RequestParam(required = false) Integer status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(menuItemService.listForRestaurant(restaurantId, categoryId, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item by ID")
    public ResponseEntity<MenuItemResponse> getById(@PathVariable String restaurantId, @PathVariable Long id) {
        return ResponseEntity.ok(menuItemService.getById(restaurantId, id));
    }

    @PostMapping
    @Operation(summary = "Create menu item")
    public ResponseEntity<MenuItemResponse> create(@PathVariable String restaurantId,
                                                   @Valid @RequestBody CreateMenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuItemService.create(restaurantId, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update menu item")
    public ResponseEntity<MenuItemResponse> update(@PathVariable String restaurantId,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody UpdateMenuItemRequest request) {
        return ResponseEntity.ok(menuItemService.update(restaurantId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete menu item")
    public ResponseEntity<Void> delete(@PathVariable String restaurantId, @PathVariable Long id) {
        menuItemService.delete(restaurantId, id);
        return ResponseEntity.noContent().build();
    }
}
