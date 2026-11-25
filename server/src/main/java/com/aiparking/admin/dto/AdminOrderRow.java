package com.aiparking.admin.dto;

import java.time.LocalDateTime;

/** 订单查询投影（与 SQL 列一一对应，状态文案在服务层补） */
public record AdminOrderRow(
        Long id,
        String orderNo,
        String username,
        String lotName,
        Integer amountCents,
        Integer billedMinutes,
        Integer status,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}
