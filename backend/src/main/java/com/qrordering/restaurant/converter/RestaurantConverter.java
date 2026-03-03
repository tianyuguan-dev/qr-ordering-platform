package com.qrordering.restaurant.converter;

import com.qrordering.common.dto.StatusInfo;
import com.qrordering.restaurant.dto.response.RestaurantResponse;
import com.qrordering.restaurant.entity.Restaurant;
import com.qrordering.restaurant.enums.RestaurantStatus;
import org.springframework.stereotype.Component;

/**
 * Restaurant Converter
 *
 * Handles conversion between Restaurant entity and DTOs.
 * Separates object mapping logic from business logic for better maintainability.
 */
@Component
public class RestaurantConverter {

    /**
     * Convert Restaurant entity to Response DTO
     *
     * @param restaurant the restaurant entity
     * @return restaurant response DTO
     */
    public RestaurantResponse toResponse(Restaurant restaurant) {
        if (restaurant == null) {
            return null;
        }

        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .description(restaurant.getDescription())
                .logoUrl(restaurant.getLogoUrl())
                .address(restaurant.getAddress())
                .phone(restaurant.getPhone())
                .status(toStatusInfo(restaurant.getStatus()))
                .createdAt(restaurant.getCreatedAt())
                .updatedAt(restaurant.getUpdatedAt())
                .build();
    }

    /**
     * Convert RestaurantStatus enum to StatusInfo DTO
     *
     * @param status the restaurant status enum
     * @return status info DTO
     */
    private StatusInfo toStatusInfo(RestaurantStatus status) {
        if (status == null) {
            return null;
        }

        return StatusInfo.builder()
                .code(status.getCode())
                .name(status.name())
                .description(status.getDescription())
                .build();
    }
}
