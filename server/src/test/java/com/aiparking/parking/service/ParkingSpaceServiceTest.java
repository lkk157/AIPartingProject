package com.aiparking.parking.service;

import com.aiparking.common.BusinessException;
import com.aiparking.common.NotFoundException;
import com.aiparking.parking.dto.BatchSpaceRequest;
import com.aiparking.parking.dto.SpaceStatusChangeRequest;
import com.aiparking.parking.dto.SpaceVO;
import com.aiparking.parking.entity.ParkingLot;
import com.aiparking.parking.entity.ParkingSpace;
import com.aiparking.parking.entity.ParkingSpaceStatusLog;
import com.aiparking.parking.mapper.ParkingLotMapper;
import com.aiparking.parking.mapper.ParkingSpaceMapper;
import com.aiparking.parking.mapper.ParkingSpaceStatusLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingSpaceServiceTest {

    @Mock
    private ParkingSpaceMapper spaceMapper;
    @Mock
    private ParkingSpaceStatusLogMapper statusLogMapper;
    @Mock
    private ParkingLotMapper lotMapper;
    @Mock
    private ParkingLotService parkingLotService;

    private ParkingSpaceService newService() {
        return new ParkingSpaceService(spaceMapper, statusLogMapper, lotMapper, parkingLotService);
    }

    private ParkingSpace space(int status) {
        ParkingSpace space = new ParkingSpace();
        space.setId(1L);
        space.setLotId(2L);
        space.setSpaceNo("A-001");
        space.setType("NORMAL");
        space.setStatus(status);
        return space;
    }

    private SpaceStatusChangeRequest statusRequest(int status) {
        SpaceStatusChangeRequest request = new SpaceStatusChangeRequest();
        request.setStatus(status);
        request.setReason("人工调整");
        return request;
    }

    @Test
    void should_reject_illegal_transition_and_skip_update() {
        when(spaceMapper.selectById(1L)).thenReturn(space(3)); // 维护

        assertThatThrownBy(() -> newService().changeStatus(1L, statusRequest(1), 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("维护")
                .hasMessageContaining("占用");
        verify(spaceMapper, never()).updateStatusIfMatch(anyLong(), anyInt(), anyInt());
        verify(statusLogMapper, never()).insert(any(ParkingSpaceStatusLog.class));
    }

    @Test
    void should_throw_conflict_when_status_changed_concurrently() {
        when(spaceMapper.selectById(1L)).thenReturn(space(0)); // 空闲
        when(spaceMapper.updateStatusIfMatch(1L, 0, 1)).thenReturn(0);

        assertThatThrownBy(() -> newService().changeStatus(1L, statusRequest(1), 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被其他操作变更");
        verify(statusLogMapper, never()).insert(any(ParkingSpaceStatusLog.class));
    }

    @Test
    void should_update_status_and_write_log_when_transition_valid() {
        when(spaceMapper.selectById(1L)).thenReturn(space(0));
        when(spaceMapper.updateStatusIfMatch(1L, 0, 1)).thenReturn(1);

        SpaceVO vo = newService().changeStatus(1L, statusRequest(1), 9L);

        assertThat(vo.status()).isEqualTo(1);
        assertThat(vo.statusText()).isEqualTo("占用");

        ArgumentCaptor<ParkingSpaceStatusLog> captor = ArgumentCaptor.forClass(ParkingSpaceStatusLog.class);
        verify(statusLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isZero();
        assertThat(captor.getValue().getToStatus()).isEqualTo(1);
        assertThat(captor.getValue().getOperatorId()).isEqualTo(9L);
        assertThat(captor.getValue().getReason()).isEqualTo("人工调整");
    }

    @Test
    void should_throw_not_found_when_space_missing() {
        when(spaceMapper.selectById(8L)).thenReturn(null);

        assertThatThrownBy(() -> newService().changeStatus(8L, statusRequest(1), 9L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("车位不存在");
    }

    @Test
    void should_reject_batch_create_when_space_numbers_exist() {
        when(spaceMapper.selectCount(any())).thenReturn(2L);
        when(spaceMapper.selectList(any())).thenReturn(List.of(space(0), space(0)));

        BatchSpaceRequest request = new BatchSpaceRequest();
        request.setCount(3);
        request.setPrefix("A");

        assertThatThrownBy(() -> newService().batchCreate(2L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("车位编号已存在");
        verify(spaceMapper, never()).insert(any(ParkingSpace.class));
    }

    @Test
    void should_insert_spaces_and_sync_capacity_when_batch_create_success() {
        when(spaceMapper.selectCount(any())).thenReturn(0L);
        when(spaceMapper.insert(any(ParkingSpace.class))).thenReturn(1);
        when(spaceMapper.countByLot(2L)).thenReturn(5L);

        BatchSpaceRequest request = new BatchSpaceRequest();
        request.setCount(5);
        request.setPrefix("B2");
        request.setZone("西区");

        int created = newService().batchCreate(2L, request);

        assertThat(created).isEqualTo(5);
        verify(spaceMapper, times(5)).insert(any(ParkingSpace.class));
        ArgumentCaptor<ParkingLot> lotCaptor = ArgumentCaptor.forClass(ParkingLot.class);
        verify(lotMapper).updateById(lotCaptor.capture());
        assertThat(lotCaptor.getValue().getCapacity()).isEqualTo(5);
        verify(parkingLotService, times(1)).requireLot(eq(2L));
    }
}
