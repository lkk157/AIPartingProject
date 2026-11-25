package com.aiparking.parking.dto;

/** 停车场车位统计（批量查询用） */
public record LotSpaceStat(Long lotId, Long total, Long free) {
}
