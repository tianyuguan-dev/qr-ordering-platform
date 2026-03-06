package com.qrordering.order.service;

import com.qrordering.auth.enums.UserRole;
import com.qrordering.auth.security.RestaurantUserDetails;
import com.qrordering.menu.repository.MenuItemRepository;
import com.qrordering.order.dto.response.OrderResponse;
import com.qrordering.order.entity.OrderInfo;
import com.qrordering.order.enums.OrderStatus;
import com.qrordering.order.repository.OrderInfoRepository;
import com.qrordering.restaurant.repository.RestaurantRepository;
import com.qrordering.table.entity.TableInfo;
import com.qrordering.table.repository.TableInfoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrderService (list with status filter, auth resolution).
 * Mocks repositories and SecurityContext for tenant resolution.
 * Skipped on JDK 25+ (Mockito/ByteBuddy not yet compatible).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
@EnabledForJreRange(max = JRE.JAVA_22, disabledReason = "Mockito/ByteBuddy not yet compatible with JDK 25+")
class OrderServiceTest {

    private static final String TENANT_ID = "tenant-1";
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private OrderInfoRepository orderInfoRepository;
    @Mock
    private TableInfoRepository tableInfoRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private OrderStateMachine orderStateMachine;
    @Mock
    private com.qrordering.order.service.IdempotencyService idempotencyService;
    @Mock
    private com.qrordering.event.service.OutboxService outboxService;
    @Mock
    private com.qrordering.observability.MetricsService metricsService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderInfoRepository,
                tableInfoRepository,
                menuItemRepository,
                restaurantRepository,
                orderStateMachine,
                idempotencyService,
                outboxService,
                metricsService
        );
        RestaurantUserDetails principal = new RestaurantUserDetails(
                1L, TENANT_ID, "user", "encoded", UserRole.RESTAURANT_ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private OrderInfo minimalOrder() {
        OrderInfo o = new OrderInfo();
        o.setId(100L);
        o.setTenantId(TENANT_ID);
        o.setTableId(1L);
        o.setOrderNumber("ORD-001");
        o.setStatus(OrderStatus.CREATED);
        o.setTotalAmount(BigDecimal.TEN);
        o.setCreatedAt(LocalDateTime.now());
        o.setUpdatedAt(LocalDateTime.now());
        o.setItems(java.util.List.of());
        return o;
    }

    private TableInfo minimalTable() {
        return TableInfo.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .tableNumber("T01")
                .build();
    }

    @Nested
    @DisplayName("list")
    class ListOrders {

        @Test
        void whenStatusNull_callsFindByTenantIdOrderByCreatedAtDesc() {
            Page<OrderInfo> page = new PageImpl<>(java.util.List.of(minimalOrder()));
            when(orderInfoRepository.findByTenantIdOrderByCreatedAtDesc(eq(TENANT_ID), eq(PAGEABLE))).thenReturn(page);
            when(tableInfoRepository.findAllById(any())).thenReturn(java.util.List.of(minimalTable()));

            Page<OrderResponse> result = orderService.list("me", null, PAGEABLE);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(1);
            verify(orderInfoRepository).findByTenantIdOrderByCreatedAtDesc(TENANT_ID, PAGEABLE);
        }

        @Test
        void whenStatusEmpty_callsFindByTenantIdOrderByCreatedAtDesc() {
            Page<OrderInfo> page = new PageImpl<>(java.util.List.of(minimalOrder()));
            when(orderInfoRepository.findByTenantIdOrderByCreatedAtDesc(eq(TENANT_ID), eq(PAGEABLE))).thenReturn(page);
            when(tableInfoRepository.findAllById(any())).thenReturn(java.util.List.of(minimalTable()));

            orderService.list("me", java.util.List.of(), PAGEABLE);

            verify(orderInfoRepository).findByTenantIdOrderByCreatedAtDesc(TENANT_ID, PAGEABLE);
        }

        @Test
        void whenStatusValid_callsFindByTenantIdAndStatusIn() {
            Page<OrderInfo> page = new PageImpl<>(java.util.List.of(minimalOrder()));
            when(orderInfoRepository.findByTenantIdAndStatusInOrderByCreatedAtDesc(
                    eq(TENANT_ID), eq(java.util.List.of(OrderStatus.CREATED, OrderStatus.CONFIRMED)), eq(PAGEABLE))).thenReturn(page);
            when(tableInfoRepository.findAllById(any())).thenReturn(java.util.List.of(minimalTable()));

            orderService.list("me", java.util.List.of(1, 2), PAGEABLE);

            verify(orderInfoRepository).findByTenantIdAndStatusInOrderByCreatedAtDesc(
                    TENANT_ID, java.util.List.of(OrderStatus.CREATED, OrderStatus.CONFIRMED), PAGEABLE);
        }

        @Test
        void whenStatusContainsInvalidCodes_filtersAndUsesValidOnly() {
            Page<OrderInfo> page = new PageImpl<>(java.util.List.of(minimalOrder()));
            when(orderInfoRepository.findByTenantIdAndStatusInOrderByCreatedAtDesc(
                    eq(TENANT_ID), eq(java.util.List.of(OrderStatus.CREATED)), eq(PAGEABLE))).thenReturn(page);
            when(tableInfoRepository.findAllById(any())).thenReturn(java.util.List.of(minimalTable()));

            orderService.list("me", java.util.List.of(1, 99, 999), PAGEABLE);

            verify(orderInfoRepository).findByTenantIdAndStatusInOrderByCreatedAtDesc(
                    TENANT_ID, java.util.List.of(OrderStatus.CREATED), PAGEABLE);
        }

        @Test
        void whenAllStatusCodesInvalid_fallsBackToFindByTenantId() {
            Page<OrderInfo> page = new PageImpl<>(java.util.List.of(minimalOrder()));
            when(orderInfoRepository.findByTenantIdOrderByCreatedAtDesc(eq(TENANT_ID), eq(PAGEABLE))).thenReturn(page);
            when(tableInfoRepository.findAllById(any())).thenReturn(java.util.List.of(minimalTable()));

            orderService.list("me", java.util.List.of(0, 99), PAGEABLE);

            verify(orderInfoRepository).findByTenantIdOrderByCreatedAtDesc(TENANT_ID, PAGEABLE);
        }
    }
}
