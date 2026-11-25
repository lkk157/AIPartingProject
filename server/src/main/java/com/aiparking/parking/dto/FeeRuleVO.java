package com.aiparking.parking.dto;

public record FeeRuleVO(
        Long id,
        Long lotId,
        String ruleName,
        Integer freeMinutes,
        Integer firstHourCents,
        Integer extraPerHourCents,
        Integer dailyCapCents,
        Integer enabled
) {
}
