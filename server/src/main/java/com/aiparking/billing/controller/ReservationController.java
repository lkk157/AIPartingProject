package com.aiparking.billing.controller;

import com.aiparking.billing.dto.CreateReservationRequest;
import com.aiparking.billing.dto.ReservationVO;
import com.aiparking.billing.service.ReservationService;
import com.aiparking.common.ApiResponse;
import com.aiparking.security.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ApiResponse<ReservationVO> create(@Valid @RequestBody CreateReservationRequest request) {
        return ApiResponse.ok(reservationService.create(UserContext.currentUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<ReservationVO>> list() {
        return ApiResponse.ok(reservationService.list(UserContext.currentUserId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(UserContext.currentUserId(), id);
        return ApiResponse.ok(null);
    }
}
