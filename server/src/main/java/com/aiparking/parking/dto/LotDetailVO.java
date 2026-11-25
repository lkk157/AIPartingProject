package com.aiparking.parking.dto;

import java.util.List;

public record LotDetailVO(
        LotVO lot,
        long spaceCount,
        long freeCount,
        long occupiedCount,
        long reservedCount,
        long maintenanceCount,
        FeeRuleVO enabledRule,
        List<Long> adminIds
) {
}
