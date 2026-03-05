package com.qrordering.order.repository;

import com.qrordering.order.entity.OrderInfo;
import com.qrordering.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderInfoRepository extends JpaRepository<OrderInfo, Long> {

    boolean existsByOrderNumber(String orderNumber);

    Page<OrderInfo> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Page<OrderInfo> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, OrderStatus status, Pageable pageable);

    /** Count active orders for table (to decide if table can be set back to AVAILABLE). */
    long countByTenantIdAndTableIdAndStatusIn(String tenantId, Long tableId, List<OrderStatus> statusIn);

    /** List active orders for table (for checkout-by-table summary). */
    List<OrderInfo> findByTenantIdAndTableIdAndStatusInOrderByCreatedAtAsc(
            String tenantId, Long tableId, List<OrderStatus> statusIn);
}
