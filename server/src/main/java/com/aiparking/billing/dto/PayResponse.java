package com.aiparking.billing.dto;

import java.time.LocalDateTime;

public record PayResponse(
        Long orderId,
        String orderNo,
        String paymentNo,
        Integer amountCents,
        Integer status,
        LocalDateTime paidAt
) {
}
