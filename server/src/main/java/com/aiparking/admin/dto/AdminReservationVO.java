package com.aiparking.admin.dto;

import java.time.LocalDateTime;

public record AdminReservationVO(
        Long id,
        String username,
        String lotName,
        String spaceNo,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer status,
        String statusText
) {
}
