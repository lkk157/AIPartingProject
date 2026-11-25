package com.aiparking.parking.dto;

public record SpaceVO(
        Long id,
        Long lotId,
        String spaceNo,
        String zone,
        String type,
        Integer status,
        String statusText
) {
}
