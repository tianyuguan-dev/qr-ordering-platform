package com.qrordering.menu.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * Menu item status: 1=AVAILABLE, 2=SOLD_OUT, 3=INACTIVE
 */
@Getter
public enum MenuItemStatus {
    AVAILABLE(1, "Available"),
    SOLD_OUT(2, "Sold Out"),
    INACTIVE(3, "Inactive");

    private final int code;
    private final String description;

    MenuItemStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static MenuItemStatus fromCode(Integer code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid MenuItemStatus code: " + code));
    }
}
