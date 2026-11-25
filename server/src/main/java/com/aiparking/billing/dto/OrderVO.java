package com.aiparking.billing.dto;

import java.time.LocalDateTime;

public record OrderVO(
        Long id,
        String orderNo,
        Long lotId,
        Integer amountCents,
        Integer billedMinutes,
        Integer freeApplied,
        Integer status,
        String statusText,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}
