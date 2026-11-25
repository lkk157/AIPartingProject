package com.aiparking.parking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BatchSpaceRequest {

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "至少创建 1 个车位")
    @Max(value = 200, message = "单次最多创建 200 个车位")
    private Integer count;

    @NotBlank(message = "编号前缀不能为空")
    @Size(max = 10, message = "前缀最长 10 个字符")
    private String prefix;

    @Size(max = 20, message = "分区最长 20 个字符")
    private String zone;

    @Pattern(regexp = "NORMAL|CHARGING|DISABLED", message = "车位类型只能为 NORMAL/CHARGING/DISABLED")
    private String type;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
