package com.qrordering.order.enums;

import lombok.Getter;

import java.util.Arrays;

/** Order status: 1=CREATED, 2=CONFIRMED, 3=PREPARING, 4=READY, 5=SERVED, 6=COMPLETED, 7=CANCELLED */
@Getter
public enum OrderStatus {
    CREATED(1, "Created"),
    CONFIRMED(2, "Confirmed"),
    PREPARING(3, "Preparing"),
    READY(4, "Ready"),
    SERVED(5, "Served"),
    COMPLETED(6, "Completed"),
    CANCELLED(7, "Cancelled");

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static OrderStatus fromCode(Integer code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid OrderStatus code: " + code));
    }
}
