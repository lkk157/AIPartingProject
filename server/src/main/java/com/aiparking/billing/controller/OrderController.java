package com.aiparking.billing.controller;

import com.aiparking.billing.dto.OrderVO;
import com.aiparking.billing.dto.PayResponse;
import com.aiparking.billing.service.OrderService;
import com.aiparking.common.ApiResponse;
import com.aiparking.common.PageResult;
import com.aiparking.security.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<PageResult<OrderVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(orderService.page(UserContext.currentUserId(), page, size));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<PayResponse> pay(@PathVariable Long id) {
        return ApiResponse.ok(orderService.pay(UserContext.currentUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        orderService.cancel(UserContext.currentUserId(), id);
        return ApiResponse.ok(null);
    }
}
