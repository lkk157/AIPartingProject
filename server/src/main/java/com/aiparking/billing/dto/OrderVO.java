package com.aiparking.billing.dto;

import java.time.LocalDateTime;

public record OrderVO(
        Long id,
        String orderNo,
        Long lotId,
        /** 列表场景补充，下单响应中可能为 null */
        String lotName,
        String spaceNo,
        Integer amountCents,
        Integer billedMinutes,
        Integer freeApplied,
        Integer status,
        String statusText,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}
