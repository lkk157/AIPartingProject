package com.aiparking.parking.controller;

import com.aiparking.common.ApiResponse;
import com.aiparking.common.PageResult;
import com.aiparking.parking.dto.LotDetailVO;
import com.aiparking.parking.dto.LotVO;
import com.aiparking.parking.dto.SpaceVO;
import com.aiparking.parking.service.ParkingLotService;
import com.aiparking.parking.service.ParkingSpaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 车主端查询接口（需登录，任意角色）。
 */
@RestController
@RequestMapping("/api/lots")
public class LotQueryController {

    private final ParkingLotService lotService;
    private final ParkingSpaceService spaceService;

    public LotQueryController(ParkingLotService lotService, ParkingSpaceService spaceService) {
        this.lotService = lotService;
        this.spaceService = spaceService;
    }

    @GetMapping
    public ApiResponse<PageResult<LotVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String area) {
        return ApiResponse.ok(lotService.page(page, size, keyword, area, 1));
    }

    @GetMapping("/{id}")
    public ApiResponse<LotDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(lotService.detail(id));
    }

    @GetMapping("/{id}/spaces")
    public ApiResponse<List<SpaceVO>> spaces(@PathVariable Long id,
                                             @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(spaceService.listByLot(id, status));
    }
}
