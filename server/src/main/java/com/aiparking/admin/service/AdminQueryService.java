package com.aiparking.admin.service;

import com.aiparking.admin.dto.AdminOrderRow;
import com.aiparking.admin.dto.AdminOrderVO;
import com.aiparking.admin.dto.AdminReservationRow;
import com.aiparking.admin.dto.AdminReservationVO;
import com.aiparking.admin.dto.StatsVO;
import com.aiparking.admin.mapper.AdminQueryMapper;
import com.aiparking.billing.enums.OrderStatus;
import com.aiparking.billing.enums.ReservationStatus;
import com.aiparking.billing.mapper.ParkingOrderMapper;
import com.aiparking.billing.mapper.ReservationMapper;
import com.aiparking.billing.entity.Reservation;
import com.aiparking.common.PageResult;
import com.aiparking.parking.entity.ParkingSpace;
import com.aiparking.parking.enums.SpaceStatus;
import com.aiparking.parking.mapper.ParkingLotMapper;
import com.aiparking.parking.mapper.ParkingSpaceMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminQueryService {

    private final AdminQueryMapper adminQueryMapper;
    private final ParkingLotMapper lotMapper;
    private final ParkingSpaceMapper spaceMapper;
    private final ParkingOrderMapper orderMapper;
    private final ReservationMapper reservationMapper;

    public AdminQueryService(AdminQueryMapper adminQueryMapper, ParkingLotMapper lotMapper,
                             ParkingSpaceMapper spaceMapper, ParkingOrderMapper orderMapper,
                             ReservationMapper reservationMapper) {
        this.adminQueryMapper = adminQueryMapper;
        this.lotMapper = lotMapper;
        this.spaceMapper = spaceMapper;
        this.orderMapper = orderMapper;
        this.reservationMapper = reservationMapper;
    }

    public PageResult<AdminOrderVO> pageOrders(long page, long size, Integer status, String keyword) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 100);
        IPage<AdminOrderRow> p = new Page<>(safePage, safeSize);
        List<AdminOrderVO> records = adminQueryMapper.selectOrderPage(p, status, keyword).stream()
                .map(r -> new AdminOrderVO(r.id(), r.orderNo(), r.username(), r.lotName(), r.amountCents(),
                        r.billedMinutes(), r.status(), OrderStatus.fromCode(r.status()).getText(),
                        r.paidAt(), r.createdAt()))
                .toList();
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), records);
    }

    public PageResult<AdminReservationVO> pageReservations(long page, long size, Integer status) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 100);
        IPage<AdminReservationRow> p = new Page<>(safePage, safeSize);
        List<AdminReservationVO> records = adminQueryMapper.selectReservationPage(p, status).stream()
                .map(r -> new AdminReservationVO(r.id(), r.username(), r.lotName(), r.spaceNo(),
                        r.startTime(), r.endTime(), r.status(),
                        ReservationStatus.fromCode(r.status()).getText()))
                .toList();
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), records);
    }

    public StatsVO stats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return new StatsVO(
                lotMapper.selectCount(null),
                spaceMapper.selectCount(null),
                spaceMapper.selectCount(Wrappers.<ParkingSpace>lambdaQuery()
                        .eq(ParkingSpace::getStatus, SpaceStatus.FREE.getCode())),
                spaceMapper.selectCount(Wrappers.<ParkingSpace>lambdaQuery()
                        .eq(ParkingSpace::getStatus, SpaceStatus.OCCUPIED.getCode())),
                orderMapper.selectCount(Wrappers.<com.aiparking.billing.entity.ParkingOrder>lambdaQuery()
                        .ge(com.aiparking.billing.entity.ParkingOrder::getCreatedAt, todayStart)),
                orderMapper.sumPaidAmountSince(todayStart),
                reservationMapper.selectCount(Wrappers.<Reservation>lambdaQuery()
                        .eq(Reservation::getStatus, ReservationStatus.PENDING.getCode())
                        .gt(Reservation::getEndTime, LocalDateTime.now())));
    }
}
