package com.qrordering.auth.converter;

import com.qrordering.auth.dto.response.RestaurantUserResponse;
import com.qrordering.auth.entity.RestaurantUser;
import com.qrordering.auth.enums.UserRole;
import com.qrordering.common.dto.StatusInfo;
import org.springframework.stereotype.Component;

/**
 * Converts RestaurantUser entity to response DTOs.
 */
@Component
public class RestaurantUserConverter {

    public RestaurantUserResponse toResponse(RestaurantUser user) {
        if (user == null) {
            return null;
        }
        return RestaurantUserResponse.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .role(toStatusInfo(user.getRole()))
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private static StatusInfo toStatusInfo(UserRole role) {
        if (role == null) {
            return null;
        }
        return StatusInfo.builder()
                .code(role.getCode())
                .name(role.name())
                .description(role.getDescription())
                .build();
    }
}
