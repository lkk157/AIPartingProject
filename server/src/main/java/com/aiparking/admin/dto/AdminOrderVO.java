package com.aiparking.admin.dto;

import java.time.LocalDateTime;

public record AdminOrderVO(
        Long id,
        String orderNo,
        String username,
        String lotName,
        Integer amountCents,
        Integer billedMinutes,
        Integer status,
        String statusText,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}
