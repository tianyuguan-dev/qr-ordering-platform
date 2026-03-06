package com.qrordering.order.service;

import com.qrordering.common.exception.BusinessException;
import com.qrordering.order.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderStateMachine")
class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
    }

    @Nested
    @DisplayName("valid transitions")
    class ValidTransitions {

        @Test
        void created_to_confirmed() {
            assertThat(stateMachine.canTransition(OrderStatus.CREATED, OrderStatus.CONFIRMED)).isTrue();
        }

        @Test
        void created_to_cancelled() {
            assertThat(stateMachine.canTransition(OrderStatus.CREATED, OrderStatus.CANCELLED)).isTrue();
        }

        @Test
        void confirmed_to_preparing() {
            assertThat(stateMachine.canTransition(OrderStatus.CONFIRMED, OrderStatus.PREPARING)).isTrue();
        }

        @Test
        void served_to_completed() {
            assertThat(stateMachine.canTransition(OrderStatus.SERVED, OrderStatus.COMPLETED)).isTrue();
        }

        @Test
        void validateTransition_doesNotThrow_forValid() {
            stateMachine.validateTransition(OrderStatus.CREATED, OrderStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("invalid transitions")
    class InvalidTransitions {

        @Test
        void created_cannot_jump_to_ready() {
            assertThat(stateMachine.canTransition(OrderStatus.CREATED, OrderStatus.READY)).isFalse();
        }

        @Test
        void completed_has_no_next_state() {
            assertThat(stateMachine.canTransition(OrderStatus.COMPLETED, OrderStatus.SERVED)).isFalse();
        }

        @Test
        void validateTransition_throws_forInvalid() {
            assertThatThrownBy(() ->
                    stateMachine.validateTransition(OrderStatus.CREATED, OrderStatus.READY))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid status change");
        }
    }

    @Nested
    @DisplayName("null handling")
    class NullHandling {

        @Test
        void canTransition_withNullTo_returnsFalse() {
            assertThat(stateMachine.canTransition(OrderStatus.CREATED, null)).isFalse();
        }

        @Test
        void validateTransition_withNullTo_throws() {
            assertThatThrownBy(() ->
                    stateMachine.validateTransition(OrderStatus.CREATED, null))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
