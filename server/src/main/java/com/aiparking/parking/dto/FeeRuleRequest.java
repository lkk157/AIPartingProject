package com.aiparking.parking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FeeRuleRequest {

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 50, message = "规则名称最长 50 个字符")
    private String ruleName;

    @NotNull(message = "免费时长不能为空")
    @Min(value = 0, message = "免费时长不能为负")
    @Max(value = 120, message = "免费时长最多 120 分钟")
    private Integer freeMinutes;

    @NotNull(message = "首小时费用不能为空")
    @Min(value = 0, message = "费用不能为负")
    private Integer firstHourCents;

    @NotNull(message = "超出费用不能为空")
    @Min(value = 0, message = "费用不能为负")
    private Integer extraPerHourCents;

    @NotNull(message = "封顶费用不能为空")
    @Min(value = 0, message = "封顶费用不能为负")
    private Integer dailyCapCents;

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
}
