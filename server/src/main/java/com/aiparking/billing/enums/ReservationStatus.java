package com.aiparking.billing.enums;

import java.util.Arrays;

public enum ReservationStatus {

    PENDING(0, "待使用"),
    COMPLETED(1, "已完成"),
    CANCELLED(2, "已取消"),
    EXPIRED(3, "已过期");

    private final int code;
    private final String text;

    ReservationStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public static ReservationStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知预约状态: " + code));
    }
}
