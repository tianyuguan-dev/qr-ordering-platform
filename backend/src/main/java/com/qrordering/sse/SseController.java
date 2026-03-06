package com.qrordering.sse;

import com.qrordering.auth.enums.UserRole;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.security.RestaurantUserDetails;
import com.qrordering.restaurant.repository.RestaurantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * SSE subscription for order events (Waiter/Kitchen real-time updates). TECH_DESIGN_V2 §6.2
 */
@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
@Tag(name = "SSE", description = "Server-Sent Events for order updates")
public class SseController {

    private final SseEmitterManager sseManager;
    private final RestaurantRepository restaurantRepository;

    @GetMapping(value = "/subscribe/{tenantId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to order events for a tenant (use tenantId=me for current restaurant)")
    public SseEmitter subscribe(
            @PathVariable String tenantId,
            @RequestHeader(value = "X-Client-Id", required = false) String clientId) {
        String resolved = resolveTenantId(tenantId);
        String client = clientId != null && !clientId.isBlank() ? clientId : UUID.randomUUID().toString();
        return sseManager.subscribe(resolved, client);
    }

    private String resolveTenantId(String tenantId) {
        if ("me".equalsIgnoreCase(tenantId)) {
            var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!(principal instanceof RestaurantUserDetails r)) {
                throw new org.springframework.security.access.AccessDeniedException("Not a restaurant user");
            }
            if (r.getRole() != UserRole.RESTAURANT_ADMIN && r.getRole() != UserRole.WAITER && r.getRole() != UserRole.KITCHEN) {
                throw new org.springframework.security.access.AccessDeniedException("Only restaurant staff can subscribe to me");
            }
            return r.getTenantId();
        }
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof PlatformAdminDetails)) {
            throw new org.springframework.security.access.AccessDeniedException("Only platform admin can subscribe to another tenant");
        }
        if (!restaurantRepository.existsById(tenantId)) {
            throw new com.qrordering.common.exception.ResourceNotFoundException("Restaurant", tenantId);
        }
        return tenantId;
    }
}
