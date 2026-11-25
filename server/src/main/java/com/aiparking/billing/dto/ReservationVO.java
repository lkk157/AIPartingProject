package com.aiparking.billing.dto;

import java.time.LocalDateTime;

public record ReservationVO(
        Long id,
        Long lotId,
        /** 列表场景补充 */
        String lotName,
        Long spaceId,
        String spaceNo,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer status,
        String statusText,
        LocalDateTime createdAt
) {
}
