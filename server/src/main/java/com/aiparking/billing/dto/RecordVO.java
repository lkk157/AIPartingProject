package com.aiparking.billing.dto;

import java.time.LocalDateTime;

public record RecordVO(
        Long id,
        Long lotId,
        Long spaceId,
        String plateNo,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        Integer status,
        String statusText
) {
}
