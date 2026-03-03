package com.qrordering.restaurant.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * Restaurant operational status enumeration
 */
@Getter
public enum RestaurantStatus {
    /**
     * Restaurant is active and accepting orders
     */
    ACTIVE(1, "Active"),

    /**
     * Restaurant is temporarily suspended
     */
    SUSPENDED(2, "Suspended"),

    /**
     * Restaurant is permanently inactive
     */
    INACTIVE(3, "Inactive");

    private final Integer code;
    private final String description;

    RestaurantStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Get enum by code
     *
     * @param code status code
     * @return RestaurantStatus enum
     * @throws IllegalArgumentException if code is invalid
     */
    public static RestaurantStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid RestaurantStatus code: " + code));
    }
}
