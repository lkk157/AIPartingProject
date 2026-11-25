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
        /** 当前空闲车位数 */
        Integer freeCount,
        /** 首小时价格（分），未配置规则时为 null */
        Integer priceFromCents,
        LocalDateTime createdAt
) {
}
