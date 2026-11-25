package com.aiparking.billing.service;

import com.aiparking.billing.dto.CreateReservationRequest;
import com.aiparking.billing.dto.ReservationVO;
import com.aiparking.billing.entity.Reservation;
import com.aiparking.billing.enums.ReservationStatus;
import com.aiparking.billing.mapper.ReservationMapper;
import com.aiparking.common.BusinessException;
import com.aiparking.common.ForbiddenException;
import com.aiparking.common.NotFoundException;
import com.aiparking.parking.entity.ParkingLot;
import com.aiparking.parking.entity.ParkingSpace;
import com.aiparking.parking.enums.SpaceStatus;
import com.aiparking.parking.mapper.ParkingSpaceMapper;
import com.aiparking.parking.service.ParkingLotService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final ParkingSpaceMapper spaceMapper;
    private final ParkingLotService parkingLotService;

    public ReservationService(ReservationMapper reservationMapper,
                              ParkingSpaceMapper spaceMapper,
                              ParkingLotService parkingLotService) {
        this.reservationMapper = reservationMapper;
        this.spaceMapper = spaceMapper;
        this.parkingLotService = parkingLotService;
    }

    /**
     * 创建预约：车位状态条件更新（FREE→RESERVED）作为并发闸门，唯一索引兜底。
     */
    @Transactional
    public ReservationVO create(Long userId, CreateReservationRequest request) {
        ParkingLot lot = parkingLotService.requireLot(request.getLotId());
        if (lot.getStatus() == null || lot.getStatus() != 1) {
            throw new BusinessException("停车场已停业，无法预约");
        }

        ParkingSpace space = spaceMapper.selectById(request.getSpaceId());
        if (space == null) {
            throw new NotFoundException("车位不存在");
        }
        if (!space.getLotId().equals(request.getLotId())) {
            throw new BusinessException("车位不属于该停车场");
        }

        LocalDateTime start = request.getStartTime();
        if (start.getMinute() != 0 || start.getSecond() != 0 || start.getNano() != 0) {
            throw new BusinessException("预约需按整点开始");
        }
        if (!start.isAfter(LocalDateTime.now())) {
            throw new BusinessException("预约开始时间必须晚于当前时间");
        }
        LocalDateTime end = start.plusHours(request.getHours());

        // 业务规则：同一车主同时只能有一条待使用预约
        // 先锁用户行，避免并发请求各自通过校验后双开；过期的待使用预约先自动作废并释放车位
        reservationMapper.lockUser(userId);
        expireStaleReservations(userId);
        Long activeReservationId = reservationMapper.findActiveIdForUpdate(userId);
        if (activeReservationId != null && activeReservationId > 0) {
            throw new BusinessException("您已有待使用的预约，请先使用或取消后再预约");
        }

        if (reservationMapper.countOverlap(space.getId(), start, end) > 0) {
            throw new BusinessException("该时段已被预约");
        }

        int moved = spaceMapper.updateStatusIfMatch(space.getId(),
                SpaceStatus.FREE.getCode(), SpaceStatus.RESERVED.getCode());
        if (moved == 0) {
            throw new BusinessException("车位当前不可预约");
        }

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setLotId(request.getLotId());
        reservation.setSpaceId(space.getId());
        reservation.setStartTime(start);
        reservation.setEndTime(end);
        reservation.setStatus(ReservationStatus.PENDING.getCode());
        try {
            reservationMapper.insert(reservation);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该时段已被预约");
        }

        return toVO(reservation, lot.getName(), space.getSpaceNo());
    }

    @Transactional
    public void cancel(Long userId, Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new NotFoundException("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作该预约");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING.getCode()) {
            throw new BusinessException("当前预约状态不可取消");
        }
        if (!reservation.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("预约已开始，无法取消");
        }

        int updated = reservationMapper.update(null, Wrappers.<Reservation>lambdaUpdate()
                .eq(Reservation::getId, reservationId)
                .eq(Reservation::getStatus, ReservationStatus.PENDING.getCode())
                .set(Reservation::getStatus, ReservationStatus.CANCELLED.getCode()));
        if (updated == 0) {
            throw new BusinessException("预约状态已变更，请刷新后重试");
        }

        if (reservationMapper.countActiveBySpace(reservation.getSpaceId()) == 0) {
            spaceMapper.updateStatusIfMatch(reservation.getSpaceId(),
                    SpaceStatus.RESERVED.getCode(), SpaceStatus.FREE.getCode());
        }
    }

    public List<ReservationVO> list(Long userId) {
        List<Reservation> reservations = reservationMapper.selectList(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getStartTime)
                .last("LIMIT 50"));
        if (reservations.isEmpty()) {
            return List.of();
        }
        List<Long> spaceIds = reservations.stream().map(Reservation::getSpaceId).distinct().toList();
        Map<Long, String> spaceNos = spaceMapper.selectBatchIds(spaceIds).stream()
                .collect(Collectors.toMap(ParkingSpace::getId, ParkingSpace::getSpaceNo, (a, b) -> a));
        List<Long> lotIds = reservations.stream().map(Reservation::getLotId).distinct().toList();
        Map<Long, String> lotNames = parkingLotService.listNamesByIds(lotIds);
        return reservations.stream()
                .map(r -> toVO(r, lotNames.get(r.getLotId()), spaceNos.get(r.getSpaceId())))
                .collect(Collectors.toList());
    }

    /** 把该用户已过结束时间但仍是「待使用」的预约置为已过期，并释放被锁定的车位 */
    private void expireStaleReservations(Long userId) {
        List<Reservation> stale = reservationMapper.selectList(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getStatus, ReservationStatus.PENDING.getCode())
                .le(Reservation::getEndTime, LocalDateTime.now()));

        for (Reservation reservation : stale) {
            int updated = reservationMapper.update(null, Wrappers.<Reservation>lambdaUpdate()
                    .eq(Reservation::getId, reservation.getId())
                    .eq(Reservation::getStatus, ReservationStatus.PENDING.getCode())
                    .set(Reservation::getStatus, ReservationStatus.EXPIRED.getCode()));
            if (updated > 0 && reservationMapper.countActiveBySpace(reservation.getSpaceId()) == 0) {
                spaceMapper.updateStatusIfMatch(reservation.getSpaceId(),
                        SpaceStatus.RESERVED.getCode(), SpaceStatus.FREE.getCode());
            }
        }
    }

    private static ReservationVO toVO(Reservation reservation, String lotName, String spaceNo) {
        ReservationStatus status = ReservationStatus.fromCode(reservation.getStatus());
        return new ReservationVO(reservation.getId(), reservation.getLotId(), lotName,
                reservation.getSpaceId(), spaceNo,
                reservation.getStartTime(), reservation.getEndTime(), status.getCode(), status.getText(),
                reservation.getCreatedAt());
    }
}
