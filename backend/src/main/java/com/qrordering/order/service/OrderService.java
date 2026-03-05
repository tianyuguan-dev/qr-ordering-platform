package com.qrordering.order.service;

import com.qrordering.auth.enums.UserRole;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.security.RestaurantUserDetails;
import com.qrordering.common.exception.BusinessException;
import com.qrordering.common.exception.IdempotencyConflictException;
import com.qrordering.common.exception.ResourceNotFoundException;
import com.qrordering.menu.entity.MenuItem;
import com.qrordering.menu.enums.MenuItemStatus;
import com.qrordering.menu.repository.MenuItemRepository;
import com.qrordering.order.dto.request.CreateOrderRequest;
import com.qrordering.order.dto.response.OrderResponse;
import com.qrordering.order.dto.response.TableCheckoutSummaryResponse;
import com.qrordering.order.entity.OrderInfo;
import com.qrordering.order.entity.OrderItem;
import com.qrordering.order.enums.OrderStatus;
import com.qrordering.order.repository.OrderInfoRepository;
import com.qrordering.restaurant.repository.RestaurantRepository;
import com.qrordering.table.entity.TableInfo;
import com.qrordering.table.enums.TableStatus;
import com.qrordering.table.repository.TableInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderInfoRepository orderInfoRepository;
    private final TableInfoRepository tableInfoRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderStateMachine orderStateMachine;
    private final IdempotencyService idempotencyService;

    private String resolveTenantId(String restaurantId) {
        if ("me".equalsIgnoreCase(restaurantId)) {
            var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!(principal instanceof RestaurantUserDetails r)) {
                throw new org.springframework.security.access.AccessDeniedException("Not a restaurant user");
            }
            if (r.getRole() != UserRole.RESTAURANT_ADMIN && r.getRole() != UserRole.WAITER && r.getRole() != UserRole.KITCHEN) {
                throw new org.springframework.security.access.AccessDeniedException("Only restaurant admin, waiter or kitchen can use /me for orders");
            }
            return r.getTenantId();
        }
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof PlatformAdminDetails)) {
            throw new org.springframework.security.access.AccessDeniedException("Only platform admin can manage another restaurant's orders");
        }
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", restaurantId);
        }
        return restaurantId;
    }

    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES = List.of(
            OrderStatus.CREATED, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
            OrderStatus.READY, OrderStatus.SERVED);

    /**
     * Get order by restaurant id and order id without auth (for public API / idempotency response).
     */
    public OrderResponse getOrderByRestaurantAndId(String restaurantId, Long orderId) {
        OrderInfo order = orderInfoRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", String.valueOf(orderId)));
        if (!order.getTenantId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Order", String.valueOf(orderId));
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponse createOrder(String restaurantId, CreateOrderRequest request, String idempotencyKey) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", restaurantId);
        }
        TableInfo table = tableInfoRepository.findById(request.getTableId())
                .orElseThrow(() -> new ResourceNotFoundException("Table", String.valueOf(request.getTableId())));
        if (!table.getTenantId().equals(restaurantId)) {
            throw new BusinessException("Table does not belong to this restaurant");
        }
        if (table.getStatus() == TableStatus.RESERVED) {
            throw new BusinessException("This table is reserved and cannot be used for ordering.");
        }

        List<Long> menuItemIds = request.getItems().stream()
                .map(CreateOrderRequest.OrderItemRequest::getMenuItemId)
                .distinct()
                .toList();
        List<MenuItem> menuItems = menuItemRepository.findAllById(menuItemIds);
        Map<Long, MenuItem> itemMap = menuItems.stream().collect(Collectors.toMap(MenuItem::getId, m -> m));

        BigDecimal newItemsTotal = BigDecimal.ZERO;
        List<OrderItem> newOrderItems = new ArrayList<>();
        for (CreateOrderRequest.OrderItemRequest req : request.getItems()) {
            MenuItem mi = itemMap.get(req.getMenuItemId());
            if (mi == null) {
                throw new ResourceNotFoundException("Menu item", String.valueOf(req.getMenuItemId()));
            }
            if (!mi.getTenantId().equals(restaurantId)) {
                throw new BusinessException("Menu item does not belong to this restaurant");
            }
            if (mi.getStatus() != MenuItemStatus.AVAILABLE) {
                throw new BusinessException("Menu item '" + mi.getName() + "' is not available");
            }
            if (req.getQuantity() == null || req.getQuantity() < 1) {
                throw new BusinessException("Invalid quantity for item '" + mi.getName() + "'");
            }
            BigDecimal unitPrice = mi.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(req.getQuantity()));
            newItemsTotal = newItemsTotal.add(subtotal);
            newOrderItems.add(OrderItem.builder()
                    .tenantId(restaurantId)
                    .menuItemId(mi.getId())
                    .quantity(req.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build());
        }

        // Each submit is a separate order so each batch has its own status (e.g. first batch SERVED, 加菜 PREPARING).
        String orderNumber = generateOrderNumber(restaurantId);
        OrderInfo order = OrderInfo.builder()
                .tenantId(restaurantId)
                .tableId(table.getId())
                .orderNumber(orderNumber)
                .status(OrderStatus.CREATED)
                .totalAmount(newItemsTotal)
                .customerNotes(request.getCustomerNotes() != null ? request.getCustomerNotes().trim() : null)
                .build();
        order = orderInfoRepository.save(order);
        for (OrderItem oi : newOrderItems) {
            oi.setOrderId(order.getId());
        }
        order.getItems().addAll(newOrderItems);
        orderInfoRepository.save(order);
        setTableOccupied(table);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                idempotencyService.storeRecord(restaurantId, idempotencyKey, order.getId());
            } catch (DataIntegrityViolationException e) {
                throw new IdempotencyConflictException(restaurantId, idempotencyKey);
            }
        }

        return toResponse(order, itemMap);
    }

    private void setTableOccupied(TableInfo table) {
        if (table.getStatus() != TableStatus.OCCUPIED) {
            table.setStatus(TableStatus.OCCUPIED);
            tableInfoRepository.save(table);
        }
    }

    private void setTableAvailableIfNeeded(String tenantId, Long tableId) {
        long activeCount = orderInfoRepository.countByTenantIdAndTableIdAndStatusIn(tenantId, tableId, ACTIVE_ORDER_STATUSES);
        if (activeCount > 0) return; // other orders still in progress for this table
        tableInfoRepository.findById(tableId).ifPresent(t -> {
            if (t.getStatus() == TableStatus.OCCUPIED) {
                t.setStatus(TableStatus.AVAILABLE);
                tableInfoRepository.save(t);
            }
        });
    }

    private String generateOrderNumber(String restaurantId) {
        String prefix = "ORD_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_";
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String candidate = prefix + suffix;
        if (orderInfoRepository.existsByOrderNumber(candidate)) {
            return generateOrderNumber(restaurantId);
        }
        return candidate;
    }

    private OrderResponse toResponse(OrderInfo order, Map<Long, MenuItem> itemMap) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(oi -> {
                    MenuItem mi = itemMap != null ? itemMap.get(oi.getMenuItemId()) : null;
                    return new OrderResponse.OrderItemResponse(
                            oi.getMenuItemId(),
                            mi != null ? mi.getName() : null,
                            oi.getQuantity(),
                            oi.getUnitPrice(),
                            oi.getSubtotal()
                    );
                })
                .toList();
        String tableNumber = tableInfoRepository.findById(order.getTableId())
                .map(TableInfo::getTableNumber)
                .orElse(null);
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .tableId(order.getTableId())
                .tableNumber(tableNumber)
                .status(order.getStatus() != null ? order.getStatus().getCode() : null)
                .totalAmount(order.getTotalAmount())
                .customerNotes(order.getCustomerNotes())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    /** Build response for a single order (loads menu item names and table number). */
    private OrderResponse toResponse(OrderInfo order) {
        List<Long> menuItemIds = order.getItems().stream()
                .map(OrderItem::getMenuItemId)
                .distinct()
                .toList();
        Map<Long, MenuItem> itemMap = menuItemIds.isEmpty() ? Map.of()
                : menuItemRepository.findAllById(menuItemIds).stream().collect(Collectors.toMap(MenuItem::getId, m -> m));
        return toResponse(order, itemMap);
    }

    public Page<OrderResponse> list(String restaurantId, Integer statusCode, Pageable pageable) {
        String tenantId = resolveTenantId(restaurantId);
        Page<OrderInfo> page = statusCode != null
                ? orderInfoRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, OrderStatus.fromCode(statusCode), pageable)
                : orderInfoRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        return page.map(this::toResponse);
    }

    public OrderResponse getById(String restaurantId, Long orderId) {
        String tenantId = resolveTenantId(restaurantId);
        OrderInfo order = orderInfoRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", String.valueOf(orderId)));
        if (!order.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Order", String.valueOf(orderId));
        }
        return toResponse(order);
    }

    /**
     * Waiter-only transitions: CONFIRMED, SERVED, COMPLETED.
     * Kitchen-only transitions: PREPARING, READY.
     * CANCELLED: any restaurant role.
     */
    private void validateRoleCanTransitionTo(OrderStatus newStatus) {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof PlatformAdminDetails) {
            return;
        }
        if (!(principal instanceof RestaurantUserDetails r)) {
            throw new BusinessException("Not a restaurant user");
        }
        UserRole role = r.getRole();
        if (role == UserRole.RESTAURANT_ADMIN) {
            return;
        }
        switch (newStatus) {
            case CONFIRMED, SERVED, COMPLETED -> {
                if (role != UserRole.WAITER) {
                    throw new BusinessException("Only waiter or admin can confirm, serve, or complete orders.");
                }
            }
            case PREPARING, READY -> {
                if (role != UserRole.KITCHEN) {
                    throw new BusinessException("Only kitchen or admin can set preparing or ready.");
                }
            }
            case CANCELLED -> { /* any restaurant role */ }
            case CREATED -> { /* not used as target */ }
            default -> { }
        }
    }

    @Transactional
    public OrderResponse updateOrderStatus(String restaurantId, Long orderId, Integer statusCode) {
        String tenantId = resolveTenantId(restaurantId);
        OrderInfo order = orderInfoRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", String.valueOf(orderId)));
        if (!order.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Order", String.valueOf(orderId));
        }
        OrderStatus newStatus = OrderStatus.fromCode(statusCode);
        orderStateMachine.validateTransition(order.getStatus(), newStatus);
        validateRoleCanTransitionTo(newStatus);
        order.setStatus(newStatus);
        order = orderInfoRepository.save(order);
        if (newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED) {
            setTableAvailableIfNeeded(order.getTenantId(), order.getTableId());
        }
        return toResponse(order);
    }

    /** Get checkout summary for a table: all active orders and combined total. */
    public TableCheckoutSummaryResponse getCheckoutSummary(String restaurantId, Long tableId) {
        String tenantId = resolveTenantId(restaurantId);
        TableInfo table = tableInfoRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table", String.valueOf(tableId)));
        if (!table.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Table", String.valueOf(tableId));
        }
        List<OrderInfo> activeOrders = orderInfoRepository.findByTenantIdAndTableIdAndStatusInOrderByCreatedAtAsc(
                tenantId, tableId, ACTIVE_ORDER_STATUSES);
        BigDecimal tableTotal = activeOrders.stream()
                .map(OrderInfo::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<OrderResponse> orderResponses = activeOrders.stream().map(this::toResponse).toList();
        return TableCheckoutSummaryResponse.builder()
                .tableId(table.getId())
                .tableNumber(table.getTableNumber())
                .orderCount(activeOrders.size())
                .tableTotal(tableTotal)
                .orders(orderResponses)
                .build();
    }

    /** Checkout table: SERVED→COMPLETED, other active→CANCELLED, then set table AVAILABLE. Returns summary of what was settled. */
    @Transactional
    public TableCheckoutSummaryResponse checkoutTable(String restaurantId, Long tableId) {
        TableCheckoutSummaryResponse summary = getCheckoutSummary(restaurantId, tableId);
        if (summary.getOrderCount() == 0) {
            return summary;
        }
        String tenantId = resolveTenantId(restaurantId);
        List<OrderInfo> activeOrders = orderInfoRepository.findByTenantIdAndTableIdAndStatusInOrderByCreatedAtAsc(
                tenantId, tableId, ACTIVE_ORDER_STATUSES);
        for (OrderInfo order : activeOrders) {
            if (order.getStatus() == OrderStatus.SERVED) {
                order.setStatus(OrderStatus.COMPLETED);
            } else {
                orderStateMachine.validateTransition(order.getStatus(), OrderStatus.CANCELLED);
                order.setStatus(OrderStatus.CANCELLED);
            }
            orderInfoRepository.save(order);
        }
        TableInfo table = tableInfoRepository.findById(tableId).orElseThrow();
        if (table.getStatus() == TableStatus.OCCUPIED) {
            table.setStatus(TableStatus.AVAILABLE);
            tableInfoRepository.save(table);
        }
        return summary;
    }
}
