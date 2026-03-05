package com.qrordering.table.enums;

import lombok.Getter;

import java.util.Arrays;

/** Table status: 1=AVAILABLE, 2=OCCUPIED, 3=RESERVED */
@Getter
public enum TableStatus {
    AVAILABLE(1, "Available"),
    OCCUPIED(2, "Occupied"),
    RESERVED(3, "Reserved");

    private final int code;
    private final String description;

    TableStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static TableStatus fromCode(Integer code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid TableStatus code: " + code));
    }
}
