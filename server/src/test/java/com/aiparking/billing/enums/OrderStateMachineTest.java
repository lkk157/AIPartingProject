package com.aiparking.billing.enums;

import com.aiparking.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

    @Test
    void should_allow_unpaid_to_paid_or_cancelled() {
        assertThat(OrderStateMachine.canTransit(OrderStatus.UNPAID, OrderStatus.PAID)).isTrue();
        assertThat(OrderStateMachine.canTransit(OrderStatus.UNPAID, OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void should_reject_transition_from_terminal_status() {
        assertThat(OrderStateMachine.canTransit(OrderStatus.PAID, OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStateMachine.canTransit(OrderStatus.PAID, OrderStatus.PAID)).isFalse();
        assertThat(OrderStateMachine.canTransit(OrderStatus.CANCELLED, OrderStatus.PAID)).isFalse();
    }

    @Test
    void should_throw_with_readable_message_when_illegal() {
        assertThatThrownBy(() -> OrderStateMachine.validate(OrderStatus.PAID, OrderStatus.CANCELLED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已支付")
                .hasMessageContaining("已取消");
    }
}
