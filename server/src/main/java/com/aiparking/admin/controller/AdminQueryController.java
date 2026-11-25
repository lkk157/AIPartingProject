package com.aiparking.admin.controller;

import com.aiparking.admin.dto.AdminOrderVO;
import com.aiparking.admin.dto.AdminReservationVO;
import com.aiparking.admin.dto.StatsVO;
import com.aiparking.admin.service.AdminQueryService;
import com.aiparking.common.ApiResponse;
import com.aiparking.common.PageResult;
import com.aiparking.security.RequireRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequireRole("SYS_ADMIN")
public class AdminQueryController {

    private final AdminQueryService adminQueryService;

    public AdminQueryController(AdminQueryService adminQueryService) {
        this.adminQueryService = adminQueryService;
    }

    @GetMapping("/orders")
    public ApiResponse<PageResult<AdminOrderVO>> orders(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminQueryService.pageOrders(page, size, status, keyword));
    }

    @GetMapping("/reservations")
    public ApiResponse<PageResult<AdminReservationVO>> reservations(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(adminQueryService.pageReservations(page, size, status));
    }

    @GetMapping("/stats/overview")
    public ApiResponse<StatsVO> overview() {
        return ApiResponse.ok(adminQueryService.stats());
    }
}
