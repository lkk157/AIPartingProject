package com.aiparking.billing.dto;

import java.time.LocalDateTime;

public record ReservationVO(
        Long id,
        Long lotId,
        Long spaceId,
        String spaceNo,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer status,
        String statusText,
        LocalDateTime createdAt
) {
}
