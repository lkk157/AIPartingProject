package com.aiparking.parking.controller;

import com.aiparking.common.ApiResponse;
import com.aiparking.parking.dto.FeeRuleRequest;
import com.aiparking.parking.dto.FeeRuleVO;
import com.aiparking.parking.service.ParkingFeeRuleService;
import com.aiparking.security.RequireRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequireRole("SYS_ADMIN")
public class AdminFeeRuleController {

    private final ParkingFeeRuleService feeRuleService;

    public AdminFeeRuleController(ParkingFeeRuleService feeRuleService) {
        this.feeRuleService = feeRuleService;
    }

    @PostMapping("/lots/{lotId}/fee-rules")
    public ApiResponse<FeeRuleVO> create(@PathVariable Long lotId,
                                         @Valid @RequestBody FeeRuleRequest request) {
        return ApiResponse.ok(feeRuleService.create(lotId, request));
    }

    @GetMapping("/lots/{lotId}/fee-rules")
    public ApiResponse<List<FeeRuleVO>> list(@PathVariable Long lotId) {
        return ApiResponse.ok(feeRuleService.list(lotId));
    }

    @PutMapping("/fee-rules/{ruleId}")
    public ApiResponse<FeeRuleVO> update(@PathVariable Long ruleId,
                                         @Valid @RequestBody FeeRuleRequest request) {
        return ApiResponse.ok(feeRuleService.update(ruleId, request));
    }

    @DeleteMapping("/fee-rules/{ruleId}")
    public ApiResponse<Void> delete(@PathVariable Long ruleId) {
        feeRuleService.delete(ruleId);
        return ApiResponse.ok(null);
    }
}
