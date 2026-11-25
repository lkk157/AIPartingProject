package com.aiparking.billing.dto;

import java.time.LocalDateTime;

/** 用户当前进行中的停车（用于首页/停车中页面） */
public record CurrentParkingVO(
        Long recordId,
        Long lotId,
        String lotName,
        Long spaceId,
        String spaceNo,
        String plateNo,
        LocalDateTime entryTime,
        long elapsedMinutes,
        Integer estimatedCents,
        boolean freeApplied,
        boolean ruleMissing
) {
}
