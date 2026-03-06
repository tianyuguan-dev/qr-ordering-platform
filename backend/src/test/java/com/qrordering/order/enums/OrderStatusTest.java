package com.qrordering.order.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for OrderStatus enum (fromCode / tryFromCode).
 */
@DisplayName("OrderStatus")
class OrderStatusTest {

    @Nested
    @DisplayName("fromCode")
    class FromCode {

        @ParameterizedTest(name = "code {0} -> {1}")
        @CsvSource({
                "1, CREATED",
                "2, CONFIRMED",
                "3, PREPARING",
                "4, READY",
                "5, SERVED",
                "6, COMPLETED",
                "7, CANCELLED"
        })
        void validCode_returnsEnum(int code, String name) {
            assertThat(OrderStatus.fromCode(code).name()).isEqualTo(name);
        }

        @Test
        void null_returnsNull() {
            assertThat(OrderStatus.fromCode(null)).isNull();
        }

        @Test
        void invalidCode_throws() {
            assertThatThrownBy(() -> OrderStatus.fromCode(99))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid OrderStatus code: 99");
        }
    }

    @Nested
    @DisplayName("tryFromCode")
    class TryFromCode {

        @Test
        void validCode_returnsPresent() {
            assertThat(OrderStatus.tryFromCode(1)).isEqualTo(Optional.of(OrderStatus.CREATED));
        }

        @Test
        void null_returnsEmpty() {
            assertThat(OrderStatus.tryFromCode(null)).isEqualTo(Optional.empty());
        }

        @Test
        void invalidCode_returnsEmpty() {
            assertThat(OrderStatus.tryFromCode(0)).isEmpty();
            assertThat(OrderStatus.tryFromCode(99)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCode")
    class GetCode {

        @Test
        void roundTrip_fromCode_getCode() {
            for (OrderStatus status : OrderStatus.values()) {
                assertThat(OrderStatus.fromCode(status.getCode())).isEqualTo(status);
            }
        }
    }
}
