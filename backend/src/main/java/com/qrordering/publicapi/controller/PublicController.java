package com.qrordering.publicapi.controller;

import com.qrordering.common.exception.IdempotencyConflictException;
import com.qrordering.common.exception.ResourceNotFoundException;
import com.qrordering.menu.entity.Category;
import com.qrordering.menu.entity.MenuItem;
import com.qrordering.menu.enums.MenuItemStatus;
import com.qrordering.menu.repository.CategoryRepository;
import com.qrordering.menu.repository.MenuItemRepository;
import com.qrordering.order.dto.request.CreateOrderRequest;
import com.qrordering.order.dto.response.OrderResponse;
import com.qrordering.order.service.IdempotencyService;
import com.qrordering.order.service.OrderService;
import com.qrordering.publicapi.dto.PublicMenuResponse;
import com.qrordering.restaurant.repository.RestaurantRepository;
import com.qrordering.table.dto.response.TableInfoResponse;
import com.qrordering.table.entity.TableInfo;
import com.qrordering.table.repository.TableInfoRepository;
import com.qrordering.restaurant.entity.Restaurant;
import com.qrordering.table.converter.TableInfoConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public/restaurants/{restaurantId}")
@RequiredArgsConstructor
@Tag(name = "Public API", description = "Customer H5: menu, tables, submit order (no auth)")
public class PublicController {

    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final TableInfoRepository tableInfoRepository;
    private final TableInfoConverter tableInfoConverter;
    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    @GetMapping("/info")
    @Operation(summary = "Get restaurant name and logo (for customer header)")
    public ResponseEntity<Map<String, String>> getRestaurantInfo(@PathVariable String restaurantId) {
        Restaurant r = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        return ResponseEntity.ok(Map.of(
                "name", r.getName() != null ? r.getName() : "",
                "logoUrl", r.getLogoUrl() != null ? r.getLogoUrl() : ""
        ));
    }

    @GetMapping("/menu")
    @Operation(summary = "Get menu (categories + available items)")
    public ResponseEntity<PublicMenuResponse> getMenu(@PathVariable String restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", restaurantId);
        }
        List<Category> categories = categoryRepository.findByTenantIdOrderBySortOrderAscNameAsc(restaurantId);
        List<MenuItem> items = menuItemRepository.findByTenantIdAndStatusOrderByCategoryIdAscNameAsc(restaurantId, MenuItemStatus.AVAILABLE);
        PublicMenuResponse response = PublicMenuResponse.builder()
                .categories(categories.stream()
                        .map(c -> new PublicMenuResponse.CategoryDto(c.getId(), c.getName(), c.getSortOrder()))
                        .toList())
                .items(items.stream()
                        .map(m -> new PublicMenuResponse.MenuItemDto(
                                m.getId(), m.getCategoryId(), m.getName(), m.getDescription(),
                                m.getPrice(), m.getImageUrl(), m.getAllergens()))
                        .toList())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tables")
    @Operation(summary = "List tables")
    public ResponseEntity<List<TableInfoResponse>> getTables(@PathVariable String restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", restaurantId);
        }
        List<TableInfo> tables = tableInfoRepository.findByTenantIdOrderByTableNumberAsc(restaurantId);
        return ResponseEntity.ok(tables.stream().map(tableInfoConverter::toResponse).toList());
    }

    @PostMapping("/orders")
    @Operation(summary = "Create order (customer submit). Send Idempotency-Key to avoid duplicate orders on retry.")
    public ResponseEntity<OrderResponse> createOrder(
            @PathVariable String restaurantId,
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingId = idempotencyService.findExistingOrderId(restaurantId, idempotencyKey);
            if (existingId.isPresent()) {
                OrderResponse existing = orderService.getOrderByRestaurantAndId(restaurantId, existingId.get());
                return ResponseEntity.ok(existing);
            }
        }

        try {
            OrderResponse order = orderService.createOrder(restaurantId, request, idempotencyKey);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IdempotencyConflictException e) {
            var existingId = idempotencyService.findExistingOrderId(e.getTenantId(), e.getIdempotencyKey());
            OrderResponse existing = orderService.getOrderByRestaurantAndId(restaurantId, existingId.orElseThrow());
            return ResponseEntity.ok(existing);
        }
    }
}
