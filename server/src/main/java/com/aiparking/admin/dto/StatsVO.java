package com.aiparking.admin.dto;

public record StatsVO(
        long lotCount,
        long spaceCount,
        long freeSpaces,
        long occupiedSpaces,
        long todayOrders,
        long todayRevenueCents,
        long pendingReservations
) {
}
