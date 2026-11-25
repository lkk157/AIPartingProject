package com.aiparking.billing.controller;

import com.aiparking.billing.dto.CurrentParkingVO;
import com.aiparking.billing.dto.EntryRequest;
import com.aiparking.billing.dto.OrderVO;
import com.aiparking.billing.dto.RecordVO;
import com.aiparking.billing.service.ParkingFlowService;
import com.aiparking.common.ApiResponse;
import com.aiparking.security.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {

    private final ParkingFlowService parkingFlowService;

    public ParkingController(ParkingFlowService parkingFlowService) {
        this.parkingFlowService = parkingFlowService;
    }

    @PostMapping("/entry")
    public ApiResponse<RecordVO> entry(@Valid @RequestBody EntryRequest request) {
        return ApiResponse.ok(parkingFlowService.entry(UserContext.currentUserId(), request));
    }

    @PostMapping("/records/{recordId}/exit")
    public ApiResponse<OrderVO> exit(@PathVariable Long recordId) {
        return ApiResponse.ok(parkingFlowService.exit(UserContext.currentUserId(), recordId));
    }

    @GetMapping("/records")
    public ApiResponse<List<RecordVO>> records() {
        return ApiResponse.ok(parkingFlowService.listRecords(UserContext.currentUserId()));
    }

    @GetMapping("/current")
    public ApiResponse<CurrentParkingVO> current() {
        return ApiResponse.ok(parkingFlowService.current(UserContext.currentUserId()));
    }
}
