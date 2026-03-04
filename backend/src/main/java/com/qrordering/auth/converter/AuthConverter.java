package com.qrordering.auth.converter;

import com.qrordering.auth.dto.response.LoginResponse;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.security.RestaurantUserDetails;
import com.qrordering.common.dto.StatusInfo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Converts auth-related types to DTOs (e.g. UserDetails → LoginResponse).
 */
@Component
public class AuthConverter {

    /**
     * Build LoginResponse from authenticated principal and access token.
     */
    public LoginResponse toLoginResponse(UserDetails principal, String accessToken) {
        return LoginResponse.builder()
                .userId(userIdFrom(principal))
                .tenantId(tenantIdFrom(principal))
                .username(plainUsernameFrom(principal))
                .role(roleInfoFrom(principal))
                .accessToken(accessToken)
                .build();
    }

    private static Long userIdFrom(UserDetails u) {
        if (u instanceof PlatformAdminDetails p) return p.getId();
        if (u instanceof RestaurantUserDetails r) return r.getId();
        return null;
    }

    private static String tenantIdFrom(UserDetails u) {
        if (u instanceof PlatformAdminDetails) return PlatformAdminDetails.PLATFORM_TENANT_ID;
        if (u instanceof RestaurantUserDetails r) return r.getTenantId();
        return null;
    }

    private static String plainUsernameFrom(UserDetails u) {
        if (u instanceof PlatformAdminDetails p) return p.getPlainUsername();
        if (u instanceof RestaurantUserDetails r) return r.getPlainUsername();
        return u.getUsername();
    }

    private static StatusInfo roleInfoFrom(UserDetails u) {
        if (u instanceof PlatformAdminDetails) {
            return StatusInfo.builder()
                    .code(0)
                    .name("PLATFORM_ADMIN")
                    .description("Platform Admin")
                    .build();
        }
        if (u instanceof RestaurantUserDetails r) {
            return StatusInfo.builder()
                    .code(r.getRole().getCode())
                    .name(r.getRole().name())
                    .description(r.getRole().getDescription())
                    .build();
        }
        return null;
    }
}
