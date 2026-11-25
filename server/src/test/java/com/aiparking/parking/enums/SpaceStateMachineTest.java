package com.aiparking.parking.enums;

import com.aiparking.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpaceStateMachineTest {

    @Test
    void should_allow_free_to_occupied_reserved_maintenance() {
        assertThat(SpaceStateMachine.canTransit(SpaceStatus.FREE, SpaceStatus.OCCUPIED)).isTrue();
        assertThat(SpaceStateMachine.canTransit(SpaceStatus.FREE, SpaceStatus.RESERVED)).isTrue();
        assertThat(SpaceStateMachine.canTransit(SpaceStatus.FREE, SpaceStatus.MAINTENANCE)).isTrue();
    }

    @Test
    void should_allow_occupied_to_free_only() {
        assertThat(SpaceStateMachine.canTransit(SpaceStatus.OCCUPIED, SpaceStatus.FREE)).isTrue();
        assertThat(SpaceStateMachine.canTransit(SpaceStatus.OCCUPIED, SpaceStatus.RESERVED)).isFalse();
        assertThat(SpaceStateMachine.canTransit(SpaceStatus.OCCUPIED, SpaceStatus.MAINTENANCE)).isFalse();
    }

    @Test
    void should_allow_reserved_to_occupied_or_free() {
        assertThat(SpaceStateMachine.canTransit(SpaceStatus.RESERVED, SpaceStatus.OCCUPIED)).isTrue();
        assertThat(SpaceStateMachine.canTransit(SpaceStatus.RESERVED, SpaceStatus.FREE)).isTrue();
        assertThat(SpaceStateMachine.canTransit(SpaceStatus.RESERVED, SpaceStatus.MAINTENANCE)).isFalse();
    }

    @Test
    void should_allow_maintenance_to_free_only() {
        assertThat(SpaceStateMachine.canTransit(SpaceStatus.MAINTENANCE, SpaceStatus.FREE)).isTrue();
        assertThat(SpaceStateMachine.canTransit(SpaceStatus.MAINTENANCE, SpaceStatus.OCCUPIED)).isFalse();
        assertThat(SpaceStateMachine.canTransit(SpaceStatus.MAINTENANCE, SpaceStatus.RESERVED)).isFalse();
    }

    @Test
    void should_reject_same_status_transition() {
        for (SpaceStatus status : SpaceStatus.values()) {
            assertThat(SpaceStateMachine.canTransit(status, status)).isFalse();
        }
    }

    @Test
    void should_throw_with_readable_message_when_illegal() {
        assertThatThrownBy(() -> SpaceStateMachine.validate(SpaceStatus.MAINTENANCE, SpaceStatus.OCCUPIED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("维护")
                .hasMessageContaining("占用");
    }
}
