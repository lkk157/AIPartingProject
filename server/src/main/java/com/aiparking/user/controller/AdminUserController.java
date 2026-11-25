package com.aiparking.user.controller;

import com.aiparking.common.ApiResponse;
import com.aiparking.common.PageResult;
import com.aiparking.security.RequireRole;
import com.aiparking.user.dto.UpdateStatusRequest;
import com.aiparking.user.dto.UserVO;
import com.aiparking.user.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequireRole("SYS_ADMIN")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<PageResult<UserVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminUserService.page(page, size, keyword));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody UpdateStatusRequest request) {
        adminUserService.updateStatus(id, request.getStatus());
        return ApiResponse.ok(null);
    }
}
