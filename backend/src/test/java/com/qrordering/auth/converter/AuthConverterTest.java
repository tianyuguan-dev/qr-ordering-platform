package com.qrordering.auth.converter;

import com.qrordering.auth.dto.response.LoginResponse;
import com.qrordering.auth.enums.UserRole;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.security.RestaurantUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AuthConverter — no Spring context, no mocks.
 * Verifies that UserDetails subtypes map to correct LoginResponse fields.
 */
@DisplayName("AuthConverter")
class AuthConverterTest {

    private AuthConverter converter;

    @BeforeEach
    void setUp() {
        converter = new AuthConverter();
    }

    @Test
    @DisplayName("platform admin: tenantId=PLATFORM, roleCode=0, roleName=PLATFORM_ADMIN, accessToken passed through")
    void platformAdmin_hasCorrectFields() {
        PlatformAdminDetails admin = new PlatformAdminDetails(10L, "sysadmin", "");

        LoginResponse resp = converter.toLoginResponse(admin, "token-abc");

        assertThat(resp.getUserId()).isEqualTo(10L);
        assertThat(resp.getTenantId()).isEqualTo("PLATFORM");
        assertThat(resp.getUsername()).isEqualTo("sysadmin");
        assertThat(resp.getAccessToken()).isEqualTo("token-abc");
        assertThat(resp.getRole()).isNotNull();
        assertThat(resp.getRole().getCode()).isEqualTo(0);
        assertThat(resp.getRole().getName()).isEqualTo("PLATFORM_ADMIN");
        assertThat(resp.getRole().getDescription()).isEqualTo("Platform Admin");
    }

    @Test
    @DisplayName("restaurant admin: tenantId=restaurant, roleCode=1, roleName=RESTAURANT_ADMIN")
    void restaurantAdmin_hasCorrectRoleCode() {
        RestaurantUserDetails user = new RestaurantUserDetails(
                5L, "tenant-abc", "admin01", "", UserRole.RESTAURANT_ADMIN);

        LoginResponse resp = converter.toLoginResponse(user, "token-xyz");

        assertThat(resp.getUserId()).isEqualTo(5L);
        assertThat(resp.getTenantId()).isEqualTo("tenant-abc");
        assertThat(resp.getUsername()).isEqualTo("admin01");
        assertThat(resp.getAccessToken()).isEqualTo("token-xyz");
        assertThat(resp.getRole().getCode()).isEqualTo(1);
        assertThat(resp.getRole().getName()).isEqualTo("RESTAURANT_ADMIN");
    }

    @Test
    @DisplayName("waiter: roleCode=2, roleName=WAITER, null accessToken allowed")
    void waiter_roleCode2_nullAccessTokenAllowed() {
        RestaurantUserDetails user = new RestaurantUserDetails(6L, "t1", "w1", "", UserRole.WAITER);

        LoginResponse resp = converter.toLoginResponse(user, null);

        assertThat(resp.getRole().getCode()).isEqualTo(2);
        assertThat(resp.getRole().getName()).isEqualTo("WAITER");
        assertThat(resp.getAccessToken()).isNull();
    }

    @Test
    @DisplayName("kitchen: roleCode=3, roleName=KITCHEN")
    void kitchen_roleCode3() {
        RestaurantUserDetails user = new RestaurantUserDetails(7L, "t1", "k1", "", UserRole.KITCHEN);

        LoginResponse resp = converter.toLoginResponse(user, null);

        assertThat(resp.getRole().getCode()).isEqualTo(3);
        assertThat(resp.getRole().getName()).isEqualTo("KITCHEN");
        assertThat(resp.getRole().getDescription()).isEqualTo("Kitchen Staff");
    }

    @Test
    @DisplayName("all restaurant roles: code and name match UserRole enum values")
    void allRoles_codeAndNameMatchEnum() {
        for (UserRole role : UserRole.values()) {
            RestaurantUserDetails user = new RestaurantUserDetails(1L, "t", "u", "", role);
            LoginResponse resp = converter.toLoginResponse(user, null);

            assertThat(resp.getRole().getCode()).isEqualTo(role.getCode());
            assertThat(resp.getRole().getName()).isEqualTo(role.name());
            assertThat(resp.getRole().getDescription()).isEqualTo(role.getDescription());
        }
    }
}
