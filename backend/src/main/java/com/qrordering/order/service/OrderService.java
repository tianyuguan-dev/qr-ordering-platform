package com.qrordering.order.service;

import com.qrordering.common.exception.BusinessException;
import com.qrordering.common.exception.ResourceNotFoundException;
import com.qrordering.menu.entity.MenuItem;
import com.qrordering.menu.enums.MenuItemStatus;
import com.qrordering.menu.repository.MenuItemRepository;
import com.qrordering.order.dto.request.CreateOrderRequest;
import com.qrordering.order.dto.response.OrderResponse;
import com.qrordering.order.entity.OrderInfo;
import com.qrordering.order.entity.OrderItem;
import com.qrordering.order.enums.OrderStatus;
import com.qrordering.order.repository.OrderInfoRepository;
import com.qrordering.restaurant.repository.RestaurantRepository;
import com.qrordering.table.entity.TableInfo;
import com.qrordering.table.repository.TableInfoRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public OrderResponse createOrder(String restaurantId, CreateOrderRequest request) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", restaurantId);
        }
        TableInfo table = tableInfoRepository.findById(request.getTableId())
                .orElseThrow(() -> new ResourceNotFoundException("Table", String.valueOf(request.getTableId())));
        if (!table.getTenantId().equals(restaurantId)) {
            throw new BusinessException("Table does not belong to this restaurant");
        }
        List<Long> menuItemIds = request.getItems().stream()
                .map(CreateOrderRequest.OrderItemRequest::getMenuItemId)
                .distinct()
                .toList();
        List<MenuItem> menuItems = menuItemRepository.findAllById(menuItemIds);
        Map<Long, MenuItem> itemMap = menuItems.stream().collect(Collectors.toMap(MenuItem::getId, m -> m));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
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
            totalAmount = totalAmount.add(subtotal);
            orderItems.add(OrderItem.builder()
                    .tenantId(restaurantId)
                    .menuItemId(mi.getId())
                    .quantity(req.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build());
        }

        String orderNumber = generateOrderNumber(restaurantId);
        OrderInfo order = OrderInfo.builder()
                .tenantId(restaurantId)
                .tableId(table.getId())
                .orderNumber(orderNumber)
                .status(OrderStatus.CREATED)
                .totalAmount(totalAmount)
                .customerNotes(request.getCustomerNotes() != null ? request.getCustomerNotes().trim() : null)
                .build();
        order = orderInfoRepository.save(order);
        for (OrderItem oi : orderItems) {
            oi.setOrderId(order.getId());
        }
        order.getItems().addAll(orderItems);
        orderInfoRepository.save(order);

        return toResponse(order, itemMap);
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
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .tableId(order.getTableId())
                .status(order.getStatus() != null ? order.getStatus().getCode() : null)
                .totalAmount(order.getTotalAmount())
                .customerNotes(order.getCustomerNotes())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
