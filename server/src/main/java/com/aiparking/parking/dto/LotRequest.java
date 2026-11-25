package com.aiparking.parking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LotRequest {

    @NotBlank(message = "停车场名称不能为空")
    @Size(max = 100, message = "名称最长 100 个字符")
    private String name;

    @NotBlank(message = "地址不能为空")
    @Size(max = 255, message = "地址最长 255 个字符")
    private String address;

    @NotBlank(message = "区域不能为空")
    @Size(max = 50, message = "区域最长 50 个字符")
    private String area;

    @Min(value = 0, message = "容量不能为负数")
    private Integer capacity;

    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;

    @Size(max = 255, message = "简介最长 255 个字符")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
