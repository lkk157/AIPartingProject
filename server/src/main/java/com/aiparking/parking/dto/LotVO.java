package com.aiparking.parking.dto;

import java.time.LocalDateTime;

public record LotVO(
        Long id,
        String name,
        String address,
        String area,
        Integer capacity,
        Integer status,
        String description,
        LocalDateTime createdAt
) {
}
