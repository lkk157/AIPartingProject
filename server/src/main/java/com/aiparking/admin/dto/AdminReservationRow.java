package com.aiparking.admin.dto;

import java.time.LocalDateTime;

/** 预约查询投影（与 SQL 列一一对应） */
public record AdminReservationRow(
        Long id,
        String username,
        String lotName,
        String spaceNo,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer status
) {
}
