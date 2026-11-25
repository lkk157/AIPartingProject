package com.aiparking.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("parking_fee_rule")
public class ParkingFeeRule {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lotId;
    private String ruleName;
    private Integer freeMinutes;
    private Integer firstHourCents;
    private Integer extraPerHourCents;
    private Integer dailyCapCents;
    /** 1 启用 / 0 停用 */
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLotId() {
        return lotId;
    }

    public void setLotId(Long lotId) {
        this.lotId = lotId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Integer getFreeMinutes() {
        return freeMinutes;
    }

    public void setFreeMinutes(Integer freeMinutes) {
        this.freeMinutes = freeMinutes;
    }

    public Integer getFirstHourCents() {
        return firstHourCents;
    }

    public void setFirstHourCents(Integer firstHourCents) {
        this.firstHourCents = firstHourCents;
    }

    public Integer getExtraPerHourCents() {
        return extraPerHourCents;
    }

    public void setExtraPerHourCents(Integer extraPerHourCents) {
        this.extraPerHourCents = extraPerHourCents;
    }

    public Integer getDailyCapCents() {
        return dailyCapCents;
    }

    public void setDailyCapCents(Integer dailyCapCents) {
        this.dailyCapCents = dailyCapCents;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
