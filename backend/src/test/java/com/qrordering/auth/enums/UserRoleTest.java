package com.qrordering.auth.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserRole")
class UserRoleTest {

    @Test
    @DisplayName("fromCode: valid codes return correct enum values")
    void fromCode_validCodes_returnCorrectEnum() {
        assertThat(UserRole.fromCode(1)).isEqualTo(UserRole.RESTAURANT_ADMIN);
        assertThat(UserRole.fromCode(2)).isEqualTo(UserRole.WAITER);
        assertThat(UserRole.fromCode(3)).isEqualTo(UserRole.KITCHEN);
    }

    @Test
    @DisplayName("fromCode: null returns null")
    void fromCode_null_returnsNull() {
        assertThat(UserRole.fromCode(null)).isNull();
    }

    @Test
    @DisplayName("fromCode: invalid code throws IllegalArgumentException")
    void fromCode_invalidCode_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> UserRole.fromCode(99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("fromCode: zero throws IllegalArgumentException")
    void fromCode_zero_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> UserRole.fromCode(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("fromName: valid names return correct enum values")
    void fromName_validNames_returnCorrectEnum() {
        assertThat(UserRole.fromName("RESTAURANT_ADMIN")).isEqualTo(UserRole.RESTAURANT_ADMIN);
        assertThat(UserRole.fromName("WAITER")).isEqualTo(UserRole.WAITER);
        assertThat(UserRole.fromName("KITCHEN")).isEqualTo(UserRole.KITCHEN);
    }

    @Test
    @DisplayName("fromName: null returns null")
    void fromName_null_returnsNull() {
        assertThat(UserRole.fromName(null)).isNull();
    }

    @Test
    @DisplayName("fromName: invalid name throws IllegalArgumentException")
    void fromName_invalidName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> UserRole.fromName("MANAGER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MANAGER");
    }

    @Test
    @DisplayName("getCode and getDescription return correct values for each role")
    void codeAndDescription_correctForAllRoles() {
        assertThat(UserRole.RESTAURANT_ADMIN.getCode()).isEqualTo(1);
        assertThat(UserRole.WAITER.getCode()).isEqualTo(2);
        assertThat(UserRole.KITCHEN.getCode()).isEqualTo(3);

        assertThat(UserRole.RESTAURANT_ADMIN.getDescription()).isEqualTo("Restaurant Admin");
        assertThat(UserRole.WAITER.getDescription()).isEqualTo("Waiter");
        assertThat(UserRole.KITCHEN.getDescription()).isEqualTo("Kitchen Staff");
    }

    @Test
    @DisplayName("fromCode and fromName are consistent: fromCode(role.getCode()) returns same role")
    void fromCode_roundTrip_consistent() {
        for (UserRole role : UserRole.values()) {
            assertThat(UserRole.fromCode(role.getCode())).isEqualTo(role);
            assertThat(UserRole.fromName(role.name())).isEqualTo(role);
        }
    }
}
