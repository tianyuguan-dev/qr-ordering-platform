package com.qrordering.order.controller;

import com.qrordering.auth.service.JwtService;
import com.qrordering.order.dto.response.OrderResponse;
import com.qrordering.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-layer test: verifies REST contract and that params are passed to service.
 * Uses minimal Spring context (WebMvcTest) and mocked OrderService.
 * Skipped on JDK 25+ (Mockito/ByteBuddy not yet compatible).
 */
@WebMvcTest(OrderController.class)
@DisplayName("OrderController")
@EnabledForJreRange(max = JRE.JAVA_22, disabledReason = "Mockito/ByteBuddy not yet compatible with JDK 25+")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(roles = "RESTAURANT_ADMIN")
    @DisplayName("GET list with status filter passes status list to service")
    void list_withStatusParams_callsServiceWithStatusList() throws Exception {
        Page<OrderResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(orderService.list(eq("me"), any(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/restaurants/me/orders")
                        .param("status", "1", "4")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(orderService).list(eq("me"), eq(List.of(1, 4)), any());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_ADMIN")
    @DisplayName("GET list without status calls service with null status")
    void list_withoutStatus_callsServiceWithNullStatus() throws Exception {
        Page<OrderResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(orderService.list(eq("me"), any(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/restaurants/me/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(orderService).list(eq("me"), eq(null), any());
    }
}
