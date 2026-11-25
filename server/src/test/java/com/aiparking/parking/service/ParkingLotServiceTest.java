package com.aiparking.parking.service;

import com.aiparking.common.NotFoundException;
import com.aiparking.parking.dto.LotRequest;
import com.aiparking.parking.dto.LotVO;
import com.aiparking.parking.entity.ParkingLot;
import com.aiparking.parking.mapper.ParkingFeeRuleMapper;
import com.aiparking.parking.mapper.ParkingLotMapper;
import com.aiparking.parking.mapper.ParkingSpaceMapper;
import com.aiparking.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingLotServiceTest {

    @Mock
    private ParkingLotMapper lotMapper;
    @Mock
    private ParkingSpaceMapper spaceMapper;
    @Mock
    private ParkingFeeRuleMapper feeRuleMapper;
    @Mock
    private UserMapper userMapper;

    private ParkingLotService newService() {
        return new ParkingLotService(lotMapper, spaceMapper, feeRuleMapper, userMapper);
    }

    private LotRequest request(String name) {
        LotRequest request = new LotRequest();
        request.setName(name);
        request.setAddress("测试地址");
        request.setArea("A区");
        return request;
    }

    @Test
    void should_throw_not_found_when_lot_missing_on_detail() {
        when(lotMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> newService().detail(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("停车场不存在");
    }

    @Test
    void should_throw_not_found_when_update_missing_lot() {
        when(lotMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> newService().update(9L, request("某停车场")))
                .isInstanceOf(NotFoundException.class);
        verify(lotMapper, never()).updateById(any(ParkingLot.class));
    }

    @Test
    void should_throw_not_found_when_delete_missing_lot() {
        when(lotMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> newService().delete(9L))
                .isInstanceOf(NotFoundException.class);
        verify(lotMapper, never()).deleteById(anyLong());
    }

    @Test
    void should_default_status_open_and_capacity_zero_when_create_without_them() {
        when(lotMapper.insert(any(ParkingLot.class))).thenAnswer(invocation -> {
            invocation.<ParkingLot>getArgument(0).setId(3L);
            return 1;
        });

        LotVO vo = newService().create(request("科技园停车场"));

        ArgumentCaptor<ParkingLot> captor = ArgumentCaptor.forClass(ParkingLot.class);
        verify(lotMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getCapacity()).isZero();
        assertThat(vo.id()).isEqualTo(3L);
        assertThat(vo.name()).isEqualTo("科技园停车场");
    }

    @Test
    void should_throw_when_bind_admin_to_missing_lot() {
        when(lotMapper.selectById(5L)).thenReturn(null);

        assertThatThrownBy(() -> newService().bindAdmin(5L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("停车场不存在");
        verify(lotMapper, never()).bindAdmin(anyLong(), anyLong());
    }
}
