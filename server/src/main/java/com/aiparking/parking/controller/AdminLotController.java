package com.aiparking.parking.controller;

import com.aiparking.common.ApiResponse;
import com.aiparking.common.PageResult;
import com.aiparking.parking.dto.BindAdminRequest;
import com.aiparking.parking.dto.LotDetailVO;
import com.aiparking.parking.dto.LotRequest;
import com.aiparking.parking.dto.LotVO;
import com.aiparking.parking.service.ParkingLotService;
import com.aiparking.security.RequireRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/lots")
@RequireRole("SYS_ADMIN")
public class AdminLotController {

    private final ParkingLotService parkingLotService;

    public AdminLotController(ParkingLotService parkingLotService) {
        this.parkingLotService = parkingLotService;
    }

    @GetMapping
    public ApiResponse<PageResult<LotVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(parkingLotService.page(page, size, keyword, area, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<LotDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(parkingLotService.detail(id));
    }

    @PostMapping
    public ApiResponse<LotVO> create(@Valid @RequestBody LotRequest request) {
        return ApiResponse.ok(parkingLotService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<LotVO> update(@PathVariable Long id, @Valid @RequestBody LotRequest request) {
        return ApiResponse.ok(parkingLotService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        parkingLotService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/admins")
    public ApiResponse<Void> bindAdmin(@PathVariable Long id, @Valid @RequestBody BindAdminRequest request) {
        parkingLotService.bindAdmin(id, request.getUserId());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}/admins/{userId}")
    public ApiResponse<Void> unbindAdmin(@PathVariable Long id, @PathVariable Long userId) {
        parkingLotService.unbindAdmin(id, userId);
        return ApiResponse.ok(null);
    }
}
