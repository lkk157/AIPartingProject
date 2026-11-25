package com.aiparking.billing.service;

import com.aiparking.billing.BillingResult;
import com.aiparking.billing.FeeCalculator;
import com.aiparking.billing.dto.CurrentParkingVO;
import com.aiparking.billing.dto.EntryRequest;
import com.aiparking.billing.dto.OrderVO;
import com.aiparking.billing.dto.RecordVO;
import com.aiparking.billing.entity.ParkingOrder;
import com.aiparking.billing.entity.ParkingRecord;
import com.aiparking.billing.entity.Reservation;
import com.aiparking.billing.enums.OrderStatus;
import com.aiparking.billing.enums.ReservationStatus;
import com.aiparking.billing.mapper.ParkingOrderMapper;
import com.aiparking.billing.mapper.ParkingRecordMapper;
import com.aiparking.billing.mapper.ReservationMapper;
import com.aiparking.common.BusinessException;
import com.aiparking.common.NotFoundException;
import com.aiparking.common.ForbiddenException;
import com.aiparking.parking.entity.ParkingFeeRule;
import com.aiparking.parking.entity.ParkingLot;
import com.aiparking.parking.entity.ParkingSpace;
import com.aiparking.parking.enums.SpaceStatus;
import com.aiparking.parking.mapper.ParkingSpaceMapper;
import com.aiparking.parking.service.ParkingFeeRuleService;
import com.aiparking.parking.service.ParkingLotService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ParkingFlowService {

    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ParkingRecordMapper recordMapper;
    private final ParkingSpaceMapper spaceMapper;
    private final ReservationMapper reservationMapper;
    private final ParkingOrderMapper orderMapper;
    private final ParkingLotService parkingLotService;
    private final ParkingFeeRuleService feeRuleService;

    public ParkingFlowService(ParkingRecordMapper recordMapper, ParkingSpaceMapper spaceMapper,
                              ReservationMapper reservationMapper, ParkingOrderMapper orderMapper,
                              ParkingLotService parkingLotService, ParkingFeeRuleService feeRuleService) {
        this.recordMapper = recordMapper;
        this.spaceMapper = spaceMapper;
        this.reservationMapper = reservationMapper;
        this.orderMapper = orderMapper;
        this.parkingLotService = parkingLotService;
        this.feeRuleService = feeRuleService;
    }

    @Transactional
    public RecordVO entry(Long userId, EntryRequest request) {
        ParkingLot lot = parkingLotService.requireLot(request.getLotId());
        if (lot.getStatus() == null || lot.getStatus() != 1) {
            throw new BusinessException("停车场已停业");
        }

        ParkingSpace space = spaceMapper.selectById(request.getSpaceId());
        if (space == null) {
            throw new NotFoundException("车位不存在");
        }
        if (!space.getLotId().equals(request.getLotId())) {
            throw new BusinessException("车位不属于该停车场");
        }

        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = null;
        if (request.getReservationId() != null) {
            reservation = reservationMapper.selectById(request.getReservationId());
            if (reservation == null) {
                throw new NotFoundException("预约不存在");
            }
            if (!reservation.getUserId().equals(userId)) {
                throw new ForbiddenException("无权使用该预约");
            }
            if (reservation.getStatus() != ReservationStatus.PENDING.getCode()) {
                throw new BusinessException("预约状态不可入场");
            }
            // 允许提前 60 分钟入场，超过预约结束时间不可入场
            if (now.isBefore(reservation.getStartTime().minusMinutes(60))
                    || now.isAfter(reservation.getEndTime())) {
                throw new BusinessException("不在预约可用时段内");
            }
            if (!reservation.getSpaceId().equals(space.getId())) {
                throw new BusinessException("预约车位与入场车位不一致");
            }
        }

        int fromStatus = space.getStatus();
        if (fromStatus != SpaceStatus.FREE.getCode() && fromStatus != SpaceStatus.RESERVED.getCode()) {
            throw new BusinessException("车位当前不可入场");
        }
        if (spaceMapper.updateStatusIfMatch(space.getId(), fromStatus, SpaceStatus.OCCUPIED.getCode()) == 0) {
            throw new BusinessException("车位已被占用，请刷新后重试");
        }

        if (reservation != null) {
            reservationMapper.update(null, Wrappers.<Reservation>lambdaUpdate()
                    .eq(Reservation::getId, reservation.getId())
                    .eq(Reservation::getStatus, ReservationStatus.PENDING.getCode())
                    .set(Reservation::getStatus, ReservationStatus.COMPLETED.getCode()));
        }

        ParkingRecord record = new ParkingRecord();
        record.setUserId(userId);
        record.setLotId(request.getLotId());
        record.setSpaceId(space.getId());
        record.setReservationId(request.getReservationId());
        record.setPlateNo(request.getPlateNo());
        record.setEntryTime(now);
        record.setStatus(0);
        recordMapper.insert(record);

        return toVO(record);
    }

    @Transactional
    public OrderVO exit(Long userId, Long recordId) {
        ParkingRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new NotFoundException("停车记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作该停车记录");
        }
        if (record.getStatus() != 0) {
            throw new BusinessException("该停车记录已离场");
        }

        ParkingFeeRule rule = feeRuleService.requireEnabledRule(record.getLotId());
        LocalDateTime exitTime = LocalDateTime.now();
        BillingResult billing = FeeCalculator.calculate(record.getEntryTime(), exitTime, rule);

        if (recordMapper.markExited(recordId, exitTime) == 0) {
            throw new BusinessException("该停车记录已离场");
        }
        spaceMapper.updateStatusIfMatch(record.getSpaceId(),
                SpaceStatus.OCCUPIED.getCode(), SpaceStatus.FREE.getCode());

        ParkingOrder order = new ParkingOrder();
        order.setOrderNo(generateNo("PO"));
        order.setUserId(userId);
        order.setRecordId(recordId);
        order.setLotId(record.getLotId());
        order.setAmountCents(billing.amountCents());
        order.setBilledMinutes(billing.billedMinutes());
        order.setFreeApplied(billing.freeApplied() ? 1 : 0);
        if (billing.amountCents() == 0) {
            order.setStatus(OrderStatus.PAID.getCode());
            order.setPaidAt(exitTime);
        } else {
            order.setStatus(OrderStatus.UNPAID.getCode());
        }
        orderMapper.insert(order);

        return toVO(order);
    }

    /** 当前进行中的停车（无则返回 null） */
    public CurrentParkingVO current(Long userId) {
        ParkingRecord record = recordMapper.selectOne(Wrappers.<ParkingRecord>lambdaQuery()
                .eq(ParkingRecord::getUserId, userId)
                .eq(ParkingRecord::getStatus, 0)
                .orderByDesc(ParkingRecord::getEntryTime)
                .last("LIMIT 1"));
        if (record == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        long elapsed = Math.max(Duration.between(record.getEntryTime(), now).toMinutes(), 0);
        String lotName = parkingLotService.listNamesByIds(List.of(record.getLotId())).get(record.getLotId());
        ParkingSpace space = spaceMapper.selectById(record.getSpaceId());
        ParkingFeeRule rule = feeRuleService.findEnabledRule(record.getLotId());

        Integer estimated = null;
        boolean freeApplied = false;
        if (rule != null) {
            BillingResult billing = FeeCalculator.calculate(record.getEntryTime(), now, rule);
            estimated = billing.amountCents();
            freeApplied = billing.freeApplied();
        }

        return new CurrentParkingVO(record.getId(), record.getLotId(), lotName, record.getSpaceId(),
                space == null ? null : space.getSpaceNo(), record.getPlateNo(), record.getEntryTime(),
                elapsed, estimated, freeApplied, rule == null);
    }

    public List<RecordVO> listRecords(Long userId) {
        return recordMapper.selectList(Wrappers.<ParkingRecord>lambdaQuery()
                        .eq(ParkingRecord::getUserId, userId)
                        .orderByDesc(ParkingRecord::getEntryTime)
                        .last("LIMIT 50"))
                .stream().map(ParkingFlowService::toVO).toList();
    }

    private static String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(NO_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    static RecordVO toVO(ParkingRecord record) {
        String statusText = record.getStatus() != null && record.getStatus() == 1 ? "已离场" : "停车中";
        return new RecordVO(record.getId(), record.getLotId(), record.getSpaceId(), record.getPlateNo(),
                record.getEntryTime(), record.getExitTime(), record.getStatus(), statusText);
    }

    public static OrderVO toVO(ParkingOrder order) {
        return toVO(order, null, null);
    }

    public static OrderVO toVO(ParkingOrder order, String lotName, String spaceNo) {
        OrderStatus status = OrderStatus.fromCode(order.getStatus());
        return new OrderVO(order.getId(), order.getOrderNo(), order.getLotId(), lotName, spaceNo,
                order.getAmountCents(), order.getBilledMinutes(), order.getFreeApplied(),
                status.getCode(), status.getText(), order.getPaidAt(), order.getCreatedAt());
    }
}
