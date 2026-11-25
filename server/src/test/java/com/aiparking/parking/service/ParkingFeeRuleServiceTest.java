package com.aiparking.parking.service;

import com.aiparking.common.NotFoundException;
import com.aiparking.parking.dto.FeeRuleRequest;
import com.aiparking.parking.entity.ParkingFeeRule;
import com.aiparking.parking.entity.ParkingLot;
import com.aiparking.parking.mapper.ParkingFeeRuleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingFeeRuleServiceTest {

    @Mock
    private ParkingFeeRuleMapper feeRuleMapper;
    @Mock
    private ParkingLotService parkingLotService;

    private ParkingFeeRuleService newService() {
        return new ParkingFeeRuleService(feeRuleMapper, parkingLotService);
    }

    private FeeRuleRequest request() {
        FeeRuleRequest request = new FeeRuleRequest();
        request.setRuleName("标准规则");
        request.setFreeMinutes(15);
        request.setFirstHourCents(600);
        request.setExtraPerHourCents(400);
        request.setDailyCapCents(4000);
        return request;
    }

    @Test
    void should_disable_existing_enabled_rules_when_create_new() {
        ParkingLot lot = new ParkingLot();
        lot.setId(1L);
        when(parkingLotService.requireLot(1L)).thenReturn(lot);
        when(feeRuleMapper.insert(any(ParkingFeeRule.class))).thenAnswer(invocation -> {
            invocation.<ParkingFeeRule>getArgument(0).setId(8L);
            return 1;
        });

        newService().create(1L, request());

        verify(feeRuleMapper).update(any(ParkingFeeRule.class), any());
        ArgumentCaptor<ParkingFeeRule> captor = ArgumentCaptor.forClass(ParkingFeeRule.class);
        verify(feeRuleMapper).insert(captor.capture());
        assertThat(captor.getValue().getEnabled()).isEqualTo(1);
        assertThat(captor.getValue().getLotId()).isEqualTo(1L);
        assertThat(captor.getValue().getFreeMinutes()).isEqualTo(15);
    }

    @Test
    void should_throw_not_found_when_create_for_missing_lot() {
        when(parkingLotService.requireLot(99L)).thenThrow(new NotFoundException("停车场不存在"));

        assertThatThrownBy(() -> newService().create(99L, request()))
                .isInstanceOf(NotFoundException.class);
        verify(feeRuleMapper, never()).insert(any(ParkingFeeRule.class));
    }

    @Test
    void should_throw_not_found_when_update_missing_rule() {
        when(feeRuleMapper.selectById(77L)).thenReturn(null);

        assertThatThrownBy(() -> newService().update(77L, request()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("收费规则不存在");
    }

    @Test
    void should_throw_not_found_when_enabled_rule_absent() {
        when(feeRuleMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> newService().requireEnabledRule(3L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("未配置收费规则");
    }
}
