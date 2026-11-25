package com.aiparking.billing.service;

import com.aiparking.billing.dto.OrderVO;
import com.aiparking.billing.dto.PayResponse;
import com.aiparking.billing.entity.ParkingOrder;
import com.aiparking.billing.entity.ParkingRecord;
import com.aiparking.billing.entity.PaymentRecord;
import com.aiparking.billing.mapper.ParkingRecordMapper;
import com.aiparking.parking.entity.ParkingSpace;
import com.aiparking.parking.mapper.ParkingSpaceMapper;
import com.aiparking.parking.service.ParkingLotService;
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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ParkingOrderMapper orderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final ParkingRecordMapper recordMapper;
    private final ParkingSpaceMapper spaceMapper;
    private final ParkingLotService parkingLotService;

    public OrderService(ParkingOrderMapper orderMapper, PaymentRecordMapper paymentRecordMapper,
                        ParkingRecordMapper recordMapper, ParkingSpaceMapper spaceMapper,
                        ParkingLotService parkingLotService) {
        this.orderMapper = orderMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.recordMapper = recordMapper;
        this.spaceMapper = spaceMapper;
        this.parkingLotService = parkingLotService;
    }

    public PageResult<OrderVO> page(Long userId, long page, long size) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 50);
        Page<ParkingOrder> result = orderMapper.selectPage(new Page<>(safePage, safeSize),
                Wrappers.<ParkingOrder>lambdaQuery()
                        .eq(ParkingOrder::getUserId, userId)
                        .orderByDesc(ParkingOrder::getId));
        List<ParkingOrder> orders = result.getRecords();
        List<OrderVO> records = orders.isEmpty() ? List.of() : enrich(orders);
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

    /** 批量补全停车场名称与车位号，避免逐条查询 */
    private List<OrderVO> enrich(List<ParkingOrder> orders) {
        Map<Long, String> lotNames = parkingLotService.listNamesByIds(
                orders.stream().map(ParkingOrder::getLotId).distinct().toList());

        List<Long> recordIds = orders.stream().map(ParkingOrder::getRecordId).distinct().toList();
        Map<Long, Long> recordToSpace = recordMapper.selectBatchIds(recordIds).stream()
                .collect(Collectors.toMap(ParkingRecord::getId, ParkingRecord::getSpaceId, (a, b) -> a));

        List<Long> spaceIds = recordToSpace.values().stream().distinct().toList();
        Map<Long, String> spaceNos = spaceIds.isEmpty() ? Map.of()
                : spaceMapper.selectBatchIds(spaceIds).stream()
                .collect(Collectors.toMap(ParkingSpace::getId, ParkingSpace::getSpaceNo, (a, b) -> a));

        return orders.stream()
                .map(order -> ParkingFlowService.toVO(order, lotNames.get(order.getLotId()),
                        spaceNos.get(recordToSpace.get(order.getRecordId()))))
                .toList();
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
