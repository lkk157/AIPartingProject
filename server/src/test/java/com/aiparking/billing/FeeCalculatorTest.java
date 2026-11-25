package com.aiparking.billing;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeeCalculatorTest {

    private static final int FREE = 15;
    private static final int FIRST_HOUR = 600;
    private static final int EXTRA_PER_HOUR = 400;
    private static final int DAILY_CAP = 4000;

    private BillingResult calc(long minutes) {
        return FeeCalculator.calculate(minutes, FREE, FIRST_HOUR, EXTRA_PER_HOUR, DAILY_CAP);
    }

    @Test
    void should_be_free_when_zero_minutes() {
        BillingResult result = calc(0);
        assertThat(result.amountCents()).isZero();
        assertThat(result.freeApplied()).isTrue();
    }

    @Test
    void should_be_free_when_within_free_minutes() {
        assertThat(calc(14).amountCents()).isZero();
        assertThat(calc(15).amountCents()).isZero();
        assertThat(calc(15).freeApplied()).isTrue();
    }

    @Test
    void should_charge_first_hour_when_one_minute_over_free() {
        BillingResult result = calc(16);
        assertThat(result.amountCents()).isEqualTo(600);
        assertThat(result.freeApplied()).isFalse();
    }

    @Test
    void should_charge_first_hour_when_exactly_one_hour() {
        assertThat(calc(60).amountCents()).isEqualTo(600);
    }

    @Test
    void should_charge_extra_hour_when_one_minute_over_one_hour() {
        assertThat(calc(61).amountCents()).isEqualTo(1000);
    }

    @Test
    void should_round_up_hours_when_partial_hour() {
        assertThat(calc(120).amountCents()).isEqualTo(1000);
        assertThat(calc(240).amountCents()).isEqualTo(1800);
    }

    @Test
    void should_apply_daily_cap_when_almost_one_day() {
        // 1439 分钟 = 24 个计费小时（向上取整），原价 9800 → 封顶 4000
        assertThat(calc(1439).amountCents()).isEqualTo(4000);
        assertThat(calc(1440).amountCents()).isEqualTo(4000);
    }

    @Test
    void should_apply_cap_per_day_when_cross_day() {
        // 1441 分钟 = 第 2 天，封顶按 2 天计算 8000
        assertThat(calc(1441).amountCents()).isEqualTo(8000);
    }

    @Test
    void should_compute_by_billing_minutes_when_calculate_from_times() {
        LocalDateTime entry = LocalDateTime.of(2026, 11, 25, 10, 0, 0);
        LocalDateTime exit = LocalDateTime.of(2026, 11, 25, 11, 1, 0);

        BillingResult result = FeeCalculator.calculate(entry, exit, rule());
        assertThat(result.billedMinutes()).isEqualTo(61);
        assertThat(result.amountCents()).isEqualTo(1000);
    }

    @Test
    void should_round_up_seconds_to_minute_when_partial_minute() {
        LocalDateTime entry = LocalDateTime.of(2026, 11, 25, 10, 0, 0);
        LocalDateTime exit = LocalDateTime.of(2026, 11, 25, 11, 0, 1);

        BillingResult result = FeeCalculator.calculate(entry, exit, rule());
        assertThat(result.billedMinutes()).isEqualTo(61);
    }

    @Test
    void should_reject_when_exit_before_entry() {
        LocalDateTime entry = LocalDateTime.of(2026, 11, 25, 11, 0, 0);
        LocalDateTime exit = LocalDateTime.of(2026, 11, 25, 10, 0, 0);

        assertThatThrownBy(() -> FeeCalculator.calculate(entry, exit, rule()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("离场时间");
    }

    @Test
    void should_return_zero_when_entry_equals_exit() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 25, 10, 0, 0);
        assertThat(FeeCalculator.calculate(time, time, rule()).amountCents()).isZero();
    }

    private static com.aiparking.parking.entity.ParkingFeeRule rule() {
        com.aiparking.parking.entity.ParkingFeeRule rule = new com.aiparking.parking.entity.ParkingFeeRule();
        rule.setFreeMinutes(FREE);
        rule.setFirstHourCents(FIRST_HOUR);
        rule.setExtraPerHourCents(EXTRA_PER_HOUR);
        rule.setDailyCapCents(DAILY_CAP);
        return rule;
    }
}
