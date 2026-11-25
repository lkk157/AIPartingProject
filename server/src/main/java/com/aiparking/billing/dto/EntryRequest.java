package com.aiparking.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class EntryRequest {

    @NotNull(message = "停车场不能为空")
    private Long lotId;

    @NotNull(message = "车位不能为空")
    private Long spaceId;

    @NotBlank(message = "车牌号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5]{5,10}$", message = "车牌号格式不正确")
    private String plateNo;

    /** 可选：使用预约入场 */
    private Long reservationId;

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

    public String getPlateNo() {
        return plateNo;
    }

    public void setPlateNo(String plateNo) {
        this.plateNo = plateNo;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }
}
