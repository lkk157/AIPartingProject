package com.aiparking.parking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SpaceStatusChangeRequest {

    @NotNull(message = "目标状态不能为空")
    @Min(value = 0, message = "状态只能为 0-3")
    @Max(value = 3, message = "状态只能为 0-3")
    private Integer status;

    @Size(max = 100, message = "变更原因最长 100 个字符")
    private String reason;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
