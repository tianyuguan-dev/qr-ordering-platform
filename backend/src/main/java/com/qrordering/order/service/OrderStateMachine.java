package com.qrordering.order.service;

import com.qrordering.common.exception.BusinessException;
import com.qrordering.order.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Validates allowed order status transitions.
 * CREATED -> CONFIRMED | CANCELLED
 * CONFIRMED -> PREPARING | CANCELLED
 * PREPARING -> READY | CANCELLED
 * READY -> SERVED
 * SERVED -> COMPLETED
 * COMPLETED, CANCELLED -> (terminal)
 */
@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING, Set.of(OrderStatus.READY, OrderStatus.CANCELLED),
            OrderStatus.READY, Set.of(OrderStatus.SERVED),
            OrderStatus.SERVED, Set.of(OrderStatus.COMPLETED),
            OrderStatus.COMPLETED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    public boolean canTransition(OrderStatus from, OrderStatus to) {
        if (to == null) return false;
        Set<OrderStatus> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public void validateTransition(OrderStatus from, OrderStatus to) {
        if (!canTransition(from, to)) {
            throw new BusinessException(
                    "Invalid status change: cannot change from " + (from != null ? from.name() : "?") + " to " + (to != null ? to.name() : "?"));
        }
    }
}
