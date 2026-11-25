package com.aiparking.billing;

import com.aiparking.parking.entity.ParkingFeeRule;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 计费引擎：免费时长 → 首小时 → 超出每小时（不足 1 小时按 1 小时）→ 每 24 小时封顶。
 * 金额单位为「分」。
 */
public final class FeeCalculator {

    private FeeCalculator() {
    }

    public static BillingResult calculate(LocalDateTime entryTime, LocalDateTime exitTime, ParkingFeeRule rule) {
        if (entryTime == null || exitTime == null) {
            throw new IllegalArgumentException("入场/离场时间不能为空");
        }
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("离场时间不能早于入场时间");
        }
        long seconds = Duration.between(entryTime, exitTime).getSeconds();
        long minutes = (seconds + 59) / 60;
        return calculate(minutes, rule.getFreeMinutes(), rule.getFirstHourCents(),
                rule.getExtraPerHourCents(), rule.getDailyCapCents());
    }

    public static BillingResult calculate(long minutes, int freeMinutes,
                                          int firstHourCents, int extraPerHourCents, int dailyCapCents) {
        if (minutes < 0) {
            throw new IllegalArgumentException("停车时长不能为负");
        }
        if (minutes <= freeMinutes) {
            return new BillingResult((int) minutes, 0, true);
        }

        long hours = (minutes + 59) / 60;
        long amount = firstHourCents + (hours - 1) * (long) extraPerHourCents;

        long days = (minutes + 1439) / 1440;
        long cap = dailyCapCents * days;
        amount = Math.min(amount, cap);

        return new BillingResult((int) minutes, (int) amount, false);
    }
}
