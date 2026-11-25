package com.aiparking.billing.service;

import com.aiparking.billing.dto.CreateReservationRequest;
import com.aiparking.billing.entity.Reservation;
import com.aiparking.billing.mapper.ReservationMapper;
import com.aiparking.common.BusinessException;
import com.aiparking.common.ForbiddenException;
import com.aiparking.parking.entity.ParkingLot;
import com.aiparking.parking.entity.ParkingSpace;
import com.aiparking.parking.mapper.ParkingSpaceMapper;
import com.aiparking.parking.service.ParkingLotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationMapper reservationMapper;
    @Mock
    private ParkingSpaceMapper spaceMapper;
    @Mock
    private ParkingLotService parkingLotService;

    private ReservationService newService() {
        return new ReservationService(reservationMapper, spaceMapper, parkingLotService);
    }

    private ParkingLot openLot() {
        ParkingLot lot = new ParkingLot();
        lot.setId(1L);
        lot.setStatus(1);
        return lot;
    }

    private ParkingSpace space(long lotId) {
        ParkingSpace space = new ParkingSpace();
        space.setId(7L);
        space.setLotId(lotId);
        space.setSpaceNo("A-001");
        space.setStatus(0);
        return space;
    }

    private CreateReservationRequest request(LocalDateTime start) {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setLotId(1L);
        request.setSpaceId(7L);
        request.setStartTime(start);
        request.setHours(2);
        return request;
    }

    private LocalDateTime tomorrowHour() {
        return LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.HOURS);
    }

    @Test
    void should_reject_when_lot_closed() {
        ParkingLot lot = openLot();
        lot.setStatus(0);
        when(parkingLotService.requireLot(1L)).thenReturn(lot);

        assertThatThrownBy(() -> newService().create(5L, request(tomorrowHour())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("停业");
    }

    @Test
    void should_reject_when_space_belongs_to_other_lot() {
        when(parkingLotService.requireLot(1L)).thenReturn(openLot());
        when(spaceMapper.selectById(7L)).thenReturn(space(2L));

        assertThatThrownBy(() -> newService().create(5L, request(tomorrowHour())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不属于该停车场");
    }

    @Test
    void should_reject_when_start_time_not_aligned_to_hour() {
        when(parkingLotService.requireLot(1L)).thenReturn(openLot());
        when(spaceMapper.selectById(7L)).thenReturn(space(1L));

        LocalDateTime start = tomorrowHour().plusMinutes(30);
        assertThatThrownBy(() -> newService().create(5L, request(start)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("整点");
    }

    @Test
    void should_reject_when_start_time_in_past() {
        when(parkingLotService.requireLot(1L)).thenReturn(openLot());
        when(spaceMapper.selectById(7L)).thenReturn(space(1L));

        LocalDateTime start = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.HOURS);
        assertThatThrownBy(() -> newService().create(5L, request(start)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("必须晚于当前时间");
    }

    @Test
    void should_reject_when_slot_overlapped() {
        when(parkingLotService.requireLot(1L)).thenReturn(openLot());
        when(spaceMapper.selectById(7L)).thenReturn(space(1L));
        when(reservationMapper.countOverlap(anyLong(), any(), any())).thenReturn(1L);

        assertThatThrownBy(() -> newService().create(5L, request(tomorrowHour())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该时段已被预约");
        verify(spaceMapper, never()).updateStatusIfMatch(anyLong(), anyInt(), anyInt());
    }

    @Test
    void should_reject_when_user_already_has_active_reservation() {
        when(parkingLotService.requireLot(1L)).thenReturn(openLot());
        when(spaceMapper.selectById(7L)).thenReturn(space(1L));
        when(reservationMapper.findActiveIdForUpdate(5L)).thenReturn(1L);

        assertThatThrownBy(() -> newService().create(5L, request(tomorrowHour())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已有待使用的预约");
        verify(reservationMapper, never()).insert(any(Reservation.class));
    }

    @Test
    void should_expire_stale_reservation_then_allow_create() {
        Reservation stale = pendingReservation(5L);
        stale.setId(99L);
        stale.setEndTime(LocalDateTime.now().minusHours(1));

        when(parkingLotService.requireLot(1L)).thenReturn(openLot());
        when(spaceMapper.selectById(7L)).thenReturn(space(1L));
        when(reservationMapper.selectList(any())).thenReturn(java.util.List.of(stale));
        when(reservationMapper.update(isNull(), any())).thenReturn(1);
        when(reservationMapper.countActiveBySpace(7L)).thenReturn(0L);
        when(spaceMapper.updateStatusIfMatch(7L, 2, 0)).thenReturn(1);
        when(reservationMapper.findActiveIdForUpdate(5L)).thenReturn(null);
        when(reservationMapper.countOverlap(anyLong(), any(), any())).thenReturn(0L);
        when(spaceMapper.updateStatusIfMatch(7L, 0, 2)).thenReturn(1);
        when(reservationMapper.insert(any(Reservation.class))).thenReturn(1);

        com.aiparking.billing.dto.ReservationVO result = newService().create(5L, request(tomorrowHour()));

        assertThat(result.spaceNo()).isEqualTo("A-001");
        // 过期预约被释放为已过期，车位回到空闲
        verify(spaceMapper).updateStatusIfMatch(7L, 2, 0);
        verify(reservationMapper).insert(any(Reservation.class));
    }

    @Test
    void should_reject_when_space_not_free_for_reservation() {
        when(parkingLotService.requireLot(1L)).thenReturn(openLot());
        when(spaceMapper.selectById(7L)).thenReturn(space(1L));
        when(reservationMapper.countOverlap(anyLong(), any(), any())).thenReturn(0L);
        when(spaceMapper.updateStatusIfMatch(7L, 0, 2)).thenReturn(0);

        assertThatThrownBy(() -> newService().create(5L, request(tomorrowHour())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("车位当前不可预约");
    }

    @Test
    void should_reject_cancel_when_not_owner() {
        Reservation reservation = pendingReservation(5L);
        when(reservationMapper.selectById(1L)).thenReturn(reservation);

        assertThatThrownBy(() -> newService().cancel(99L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("无权");
    }

    @Test
    void should_reject_cancel_when_already_cancelled() {
        Reservation reservation = pendingReservation(5L);
        reservation.setStatus(2);
        when(reservationMapper.selectById(1L)).thenReturn(reservation);

        assertThatThrownBy(() -> newService().cancel(5L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可取消");
    }

    @Test
    void should_reject_cancel_when_reservation_started() {
        Reservation reservation = pendingReservation(5L);
        reservation.setStartTime(LocalDateTime.now().minusHours(1));
        when(reservationMapper.selectById(1L)).thenReturn(reservation);

        assertThatThrownBy(() -> newService().cancel(5L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预约已开始");
    }

    private Reservation pendingReservation(long userId) {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUserId(userId);
        reservation.setLotId(1L);
        reservation.setSpaceId(7L);
        reservation.setStartTime(LocalDateTime.now().plusDays(1));
        reservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        reservation.setStatus(0);
        return reservation;
    }
}
