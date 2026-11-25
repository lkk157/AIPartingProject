package com.aiparking.billing.service;

import com.aiparking.billing.dto.OrderVO;
import com.aiparking.billing.dto.PayResponse;
import com.aiparking.billing.entity.ParkingOrder;
import com.aiparking.billing.entity.PaymentRecord;
import com.aiparking.billing.enums.OrderStateMachine;
import com.aiparking.billing.enums.OrderStatus;
import com.aiparking.billing.mapper.ParkingOrderMapper;
import com.aiparking.billing.mapper.PaymentRecordMapper;
import com.aiparking.common.BusinessException;
import com.aiparking.common.ForbiddenException;
import com.aiparking.common.NotFoundException;
import com.aiparking.common.PageResult;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ParkingOrderMapper orderMapper;
    private final PaymentRecordMapper paymentRecordMapper;

    public OrderService(ParkingOrderMapper orderMapper, PaymentRecordMapper paymentRecordMapper) {
        this.orderMapper = orderMapper;
        this.paymentRecordMapper = paymentRecordMapper;
    }

    public PageResult<OrderVO> page(Long userId, long page, long size) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 50);
        Page<ParkingOrder> result = orderMapper.selectPage(new Page<>(safePage, safeSize),
                Wrappers.<ParkingOrder>lambdaQuery()
                        .eq(ParkingOrder::getUserId, userId)
                        .orderByDesc(ParkingOrder::getId));
        List<OrderVO> records = result.getRecords().stream().map(ParkingFlowService::toVO).toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Transactional
    public PayResponse pay(Long userId, Long orderId) {
        ParkingOrder order = requireOwnOrder(userId, orderId);
        if (order.getStatus() != OrderStatus.UNPAID.getCode()) {
            throw new BusinessException("订单当前状态不可支付");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = orderMapper.updateStatusIfMatch(orderId, OrderStatus.UNPAID.getCode(),
                OrderStatus.PAID.getCode(), now);
        if (updated == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }

        PaymentRecord payment = new PaymentRecord();
        payment.setOrderId(orderId);
        payment.setPaymentNo(generateNo("PAY"));
        payment.setChannel("MOCK");
        payment.setAmountCents(order.getAmountCents());
        payment.setStatus(0);
        payment.setPaidAt(now);
        paymentRecordMapper.insert(payment);

        return new PayResponse(orderId, order.getOrderNo(), payment.getPaymentNo(),
                order.getAmountCents(), OrderStatus.PAID.getCode(), now);
    }

    @Transactional
    public void cancel(Long userId, Long orderId) {
        ParkingOrder order = requireOwnOrder(userId, orderId);
        OrderStatus from = OrderStatus.fromCode(order.getStatus());
        OrderStateMachine.validate(from, OrderStatus.CANCELLED);

        int updated = orderMapper.updateStatusIfMatch(orderId, from.getCode(),
                OrderStatus.CANCELLED.getCode(), null);
        if (updated == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }
    }

    private ParkingOrder requireOwnOrder(Long userId, Long orderId) {
        ParkingOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作该订单");
        }
        return order;
    }

    private static String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(NO_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}
