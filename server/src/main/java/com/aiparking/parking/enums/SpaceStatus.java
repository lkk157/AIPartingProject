package com.aiparking.parking.enums;

import java.util.Arrays;

public enum SpaceStatus {

    FREE(0, "空闲"),
    OCCUPIED(1, "占用"),
    RESERVED(2, "预约锁定"),
    MAINTENANCE(3, "维护");

    private final int code;
    private final String text;

    SpaceStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public static SpaceStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知车位状态: " + code));
    }
}
