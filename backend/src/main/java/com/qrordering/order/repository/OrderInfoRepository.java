package com.qrordering.order.repository;

import com.qrordering.order.entity.OrderInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderInfoRepository extends JpaRepository<OrderInfo, Long> {

    boolean existsByOrderNumber(String orderNumber);
}
