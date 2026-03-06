package com.qrordering.order.controller;

import com.qrordering.order.dto.request.UpdateOrderStatusRequest;
import com.qrordering.order.dto.response.OrderResponse;
import com.qrordering.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants/{restaurantId}/orders")
@RequiredArgsConstructor
@Tag(name = "Orders (management)", description = "List and update orders. restaurantId=me (restaurant admin) or id (platform admin).")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List orders (paginated). Filter by one or more statuses (e.g. status=1&status=4).")
    public ResponseEntity<Page<OrderResponse>> list(
            @PathVariable String restaurantId,
            @RequestParam(required = false) List<Integer> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.list(restaurantId, status, pageable));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponse> getById(
            @PathVariable String restaurantId,
            @PathVariable Long orderId) {

        return ResponseEntity.ok(orderService.getById(restaurantId, orderId));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status (state machine validated, 409 on concurrent update)")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable String restaurantId,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        return ResponseEntity.ok(orderService.updateOrderStatus(restaurantId, orderId, request.getStatus()));
    }
}
