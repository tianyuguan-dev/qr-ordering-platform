package com.qrordering.auth.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * User Role Enumeration
 *
 * Defines the roles that users can have within a restaurant.
 * Uses code-description pattern for frontend integration.
 */
@Getter
public enum UserRole {
    /**
     * Restaurant Administrator
     * Full access to all restaurant features
     */
    RESTAURANT_ADMIN(1, "Restaurant Admin"),

    /**
     * Waiter/Server
     * Can take orders and manage tables
     */
    WAITER(2, "Waiter"),

    /**
     * Kitchen Staff
     * Can view and update order status
     */
    KITCHEN(3, "Kitchen Staff");

    private final Integer code;
    private final String description;

    UserRole(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Get enum by code
     *
     * @param code role code
     * @return UserRole enum
     * @throws IllegalArgumentException if code is invalid
     */
    public static UserRole fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(role -> role.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid UserRole code: " + code));
    }

    /**
     * Get enum by name (for database conversion)
     *
     * @param name role name (e.g., "RESTAURANT_ADMIN")
     * @return UserRole enum
     * @throws IllegalArgumentException if name is invalid
     */
    public static UserRole fromName(String name) {
        if (name == null) {
            return null;
        }
        try {
            return UserRole.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UserRole name: " + name);
        }
    }
}
