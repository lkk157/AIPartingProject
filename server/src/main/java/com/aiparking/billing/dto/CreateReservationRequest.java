package com.aiparking.billing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateReservationRequest {

    @NotNull(message = "停车场不能为空")
    private Long lotId;

    @NotNull(message = "车位不能为空")
    private Long spaceId;

    @NotNull(message = "预约开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "预约时长不能为空")
    @Min(value = 1, message = "预约时长 1-4 小时")
    @Max(value = 4, message = "预约时长 1-4 小时")
    private Integer hours;

    public Long getLotId() {
        return lotId;
    }

    public void setLotId(Long lotId) {
        this.lotId = lotId;
    }

    public Long getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(Long spaceId) {
        this.spaceId = spaceId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Integer getHours() {
        return hours;
    }

    public void setHours(Integer hours) {
        this.hours = hours;
    }
}
