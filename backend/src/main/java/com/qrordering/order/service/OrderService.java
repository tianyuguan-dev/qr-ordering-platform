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
import com.qrordering.event.dto.OrderCreatedEventPayload;
import com.qrordering.event.dto.OrderStatusChangedEventPayload;
import com.qrordering.event.service.OutboxService;
import com.qrordering.observability.MetricsService;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderInfoRepository orderInfoRepository;
    private final TableInfoRepository tableInfoRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderStateMachine orderStateMachine;
    private final IdempotencyService idempotencyService;
    private final OutboxService outboxService;
    private final MetricsService metricsService;

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
        MDC.put("tenantId", restaurantId);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            MDC.put("idempotencyKey", idempotencyKey);
        }
        log.info("Creating order: tableId={}, itemCount={}", request.getTableId(), request.getItems().size());

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

        String tableNumber = tableInfoRepository.findById(table.getId()).map(TableInfo::getTableNumber).orElse(null);
        List<OrderCreatedEventPayload.OrderItemDto> itemDtos = newOrderItems.stream()
                .map(oi -> {
                    MenuItem mi = itemMap.get(oi.getMenuItemId());
                    return new OrderCreatedEventPayload.OrderItemDto(
                            oi.getMenuItemId(),
                            mi != null ? mi.getName() : null,
                            oi.getQuantity(),
                            oi.getUnitPrice(),
                            oi.getSubtotal());
                })
                .toList();
        OrderCreatedEventPayload createdPayload = OrderCreatedEventPayload.builder()
                .tenantId(restaurantId)
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .tableId(table.getId())
                .tableNumber(tableNumber)
                .totalAmount(newItemsTotal)
                .items(itemDtos)
                .occurredAt(Instant.now())
                .build();
        outboxService.publish("OrderCreatedEvent", restaurantId, order.getId(), createdPayload);
        metricsService.recordOrderCreated(restaurantId);

        MDC.put("orderId", order.getId().toString());
        log.info("Order created successfully: orderNumber={}, totalAmount={}", order.getOrderNumber(), order.getTotalAmount());
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
        return toResponse(order, itemMap, null);
    }

    /**
     * Build an OrderResponse using pre-loaded lookup maps to avoid N+1 queries.
     * Pass null for tableNumberMap to fall back to a single DB lookup (for single-order use cases).
     */
    private OrderResponse toResponse(OrderInfo order, Map<Long, MenuItem> itemMap, Map<Long, String> tableNumberMap) {
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
        String tableNumber = tableNumberMap != null
                ? tableNumberMap.get(order.getTableId())
                : tableInfoRepository.findById(order.getTableId()).map(TableInfo::getTableNumber).orElse(null);
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

    public Page<OrderResponse> list(String restaurantId, List<Integer> statusCodes, Pageable pageable) {
        String tenantId = resolveTenantId(restaurantId);
        Page<OrderInfo> page;
        if (statusCodes != null && !statusCodes.isEmpty()) {
            List<OrderStatus> statuses = statusCodes.stream()
                    .map(OrderStatus::tryFromCode)
                    .flatMap(Optional::stream)
                    .toList();
            if (statuses.isEmpty()) {
                page = orderInfoRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
            } else {
                page = orderInfoRepository.findByTenantIdAndStatusInOrderByCreatedAtDesc(tenantId, statuses, pageable);
            }
        } else {
            page = orderInfoRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        }

        // Batch-load table numbers and menu items to avoid N+1 queries
        List<OrderInfo> orders = page.getContent();
        if (orders.isEmpty()) {
            return page.map(o -> toResponse(o, Map.of(), Map.of()));
        }
        Set<Long> tableIds = orders.stream().map(OrderInfo::getTableId).collect(Collectors.toSet());
        Map<Long, String> tableNumberMap = tableInfoRepository.findAllById(tableIds).stream()
                .collect(Collectors.toMap(TableInfo::getId, TableInfo::getTableNumber));
        Set<Long> menuItemIds = orders.stream()
                .flatMap(o -> o.getItems().stream().map(OrderItem::getMenuItemId))
                .collect(Collectors.toSet());
        Map<Long, MenuItem> itemMap = menuItemIds.isEmpty() ? Map.of()
                : menuItemRepository.findAllById(menuItemIds).stream()
                        .collect(Collectors.toMap(MenuItem::getId, m -> m));
        return page.map(o -> toResponse(o, itemMap, tableNumberMap));
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
        MDC.put("tenantId", tenantId);
        MDC.put("orderId", orderId.toString());

        OrderInfo order = orderInfoRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", String.valueOf(orderId)));
        if (!order.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Order", String.valueOf(orderId));
        }
        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = OrderStatus.fromCode(statusCode);
        orderStateMachine.validateTransition(oldStatus, newStatus);
        validateRoleCanTransitionTo(newStatus);
        order.setStatus(newStatus);
        order = orderInfoRepository.save(order);
        if (newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED) {
            setTableAvailableIfNeeded(order.getTenantId(), order.getTableId());
        }
        OrderStatusChangedEventPayload statusPayload = OrderStatusChangedEventPayload.builder()
                .tenantId(tenantId)
                .orderId(orderId)
                .oldStatus(oldStatus.getCode())
                .newStatus(newStatus.getCode())
                .occurredAt(Instant.now())
                .build();
        outboxService.publish("OrderStatusChangedEvent", tenantId, orderId, statusPayload);
        metricsService.recordOrderStatusChanged(tenantId, oldStatus, newStatus);
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
        // All orders share the same table; batch-load menu items to avoid N+1 queries
        Map<Long, String> tableNumberMap = Map.of(tableId, table.getTableNumber());
        Set<Long> menuItemIds = activeOrders.stream()
                .flatMap(o -> o.getItems().stream().map(OrderItem::getMenuItemId))
                .collect(Collectors.toSet());
        Map<Long, MenuItem> itemMap = menuItemIds.isEmpty() ? Map.of()
                : menuItemRepository.findAllById(menuItemIds).stream()
                        .collect(Collectors.toMap(MenuItem::getId, m -> m));
        List<OrderResponse> orderResponses = activeOrders.stream()
                .map(o -> toResponse(o, itemMap, tableNumberMap))
                .toList();
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
            OrderStatus oldStatus = order.getStatus();
            OrderStatus newStatus;
            if (oldStatus == OrderStatus.SERVED) {
                newStatus = OrderStatus.COMPLETED;
            } else {
                orderStateMachine.validateTransition(oldStatus, OrderStatus.CANCELLED);
                newStatus = OrderStatus.CANCELLED;
            }
            order.setStatus(newStatus);
            orderInfoRepository.save(order);
            OrderStatusChangedEventPayload statusPayload = OrderStatusChangedEventPayload.builder()
                    .tenantId(tenantId)
                    .orderId(order.getId())
                    .oldStatus(oldStatus.getCode())
                    .newStatus(newStatus.getCode())
                    .occurredAt(Instant.now())
                    .build();
            outboxService.publish("OrderStatusChangedEvent", tenantId, order.getId(), statusPayload);
        }
        TableInfo table = tableInfoRepository.findById(tableId).orElseThrow();
        if (table.getStatus() == TableStatus.OCCUPIED) {
            table.setStatus(TableStatus.AVAILABLE);
            tableInfoRepository.save(table);
        }
        return summary;
    }
}
