package com.aiparking.parking.controller;

import com.aiparking.common.ApiResponse;
import com.aiparking.common.PageResult;
import com.aiparking.parking.dto.BatchSpaceRequest;
import com.aiparking.parking.dto.SpaceStatusChangeRequest;
import com.aiparking.parking.dto.SpaceVO;
import com.aiparking.parking.service.ParkingSpaceService;
import com.aiparking.security.RequireRole;
import com.aiparking.security.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequireRole("SYS_ADMIN")
public class AdminSpaceController {

    private final ParkingSpaceService spaceService;

    public AdminSpaceController(ParkingSpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @PostMapping("/lots/{lotId}/spaces/batch")
    public ApiResponse<Map<String, Integer>> batchCreate(@PathVariable Long lotId,
                                                         @Valid @RequestBody BatchSpaceRequest request) {
        int created = spaceService.batchCreate(lotId, request);
        return ApiResponse.ok(Map.of("created", created));
    }

    @GetMapping("/lots/{lotId}/spaces")
    public ApiResponse<PageResult<SpaceVO>> list(
            @PathVariable Long lotId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(spaceService.page(lotId, page, size, status));
    }

    @PatchMapping("/spaces/{spaceId}/status")
    public ApiResponse<SpaceVO> changeStatus(@PathVariable Long spaceId,
                                             @Valid @RequestBody SpaceStatusChangeRequest request) {
        return ApiResponse.ok(spaceService.changeStatus(spaceId, request, UserContext.currentUserId()));
    }
}
