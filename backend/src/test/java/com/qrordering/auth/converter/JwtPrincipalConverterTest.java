package com.qrordering.auth.converter;

import com.qrordering.auth.enums.UserRole;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.security.RestaurantUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtPrincipalConverter.
 * toClaimValues tests need no mocks.
 * toPrincipal tests mock the Claims interface (Mockito, skipped on JDK 25+).
 */
@DisplayName("JwtPrincipalConverter")
class JwtPrincipalConverterTest {

    private JwtPrincipalConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JwtPrincipalConverter();
    }

    // ------------------------------------------------------------------ toClaimValues

    @Nested
    @DisplayName("toClaimValues")
    class ToClaimValues {

        @Test
        @DisplayName("restaurant user: correct userId, tenantId, username, role, userType=restaurant")
        void restaurantUser_returnsRestaurantClaims() {
            RestaurantUserDetails user = new RestaurantUserDetails(
                    42L, "tenant-1", "waiter01", "", UserRole.WAITER);

            var cv = converter.toClaimValues(user);

            assertThat(cv.userId()).isEqualTo(42L);
            assertThat(cv.tenantId()).isEqualTo("tenant-1");
            assertThat(cv.username()).isEqualTo("waiter01");
            assertThat(cv.role()).isEqualTo("WAITER");
            assertThat(cv.userType()).isEqualTo(JwtPrincipalConverter.USER_TYPE_RESTAURANT);
        }

        @Test
        @DisplayName("platform admin: tenantId=PLATFORM, role=null, userType=platform")
        void platformAdmin_returnsPlatformClaims_withNullRole() {
            PlatformAdminDetails admin = new PlatformAdminDetails(1L, "superadmin", "");

            var cv = converter.toClaimValues(admin);

            assertThat(cv.userId()).isEqualTo(1L);
            assertThat(cv.tenantId()).isEqualTo(PlatformAdminDetails.PLATFORM_TENANT_ID);
            assertThat(cv.username()).isEqualTo("superadmin");
            assertThat(cv.role()).isNull();
            assertThat(cv.userType()).isEqualTo(JwtPrincipalConverter.USER_TYPE_PLATFORM);
        }

        @Test
        @DisplayName("all restaurant admin roles produce correct role claim name")
        void allRestaurantRoles_produceCorrectRoleName() {
            for (UserRole role : UserRole.values()) {
                RestaurantUserDetails user = new RestaurantUserDetails(1L, "t", "u", "", role);
                var cv = converter.toClaimValues(user);
                assertThat(cv.role()).isEqualTo(role.name());
            }
        }
    }

    // ------------------------------------------------------------------ toPrincipal (mocked Claims)

    @Nested
    @DisplayName("toPrincipal")
    @ExtendWith(MockitoExtension.class)
    @EnabledForJreRange(max = JRE.JAVA_22, disabledReason = "Mockito/ByteBuddy not yet compatible with JDK 25+")
    class ToPrincipal {

        @Mock
        Claims claims;

        @Test
        @DisplayName("platform userType returns PlatformAdminDetails with correct id and username")
        void platformType_returnsPlatformAdminDetails() {
            when(claims.get(JwtPrincipalConverter.CLAIM_USER_TYPE, String.class))
                    .thenReturn(JwtPrincipalConverter.USER_TYPE_PLATFORM);
            when(claims.get(JwtPrincipalConverter.CLAIM_USER_ID, Long.class)).thenReturn(5L);
            when(claims.get(JwtPrincipalConverter.CLAIM_USERNAME, String.class)).thenReturn("admin");

            UserDetails result = converter.toPrincipal(claims);

            assertThat(result).isInstanceOf(PlatformAdminDetails.class);
            PlatformAdminDetails p = (PlatformAdminDetails) result;
            assertThat(p.getId()).isEqualTo(5L);
            assertThat(p.getPlainUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("restaurant userType returns RestaurantUserDetails with correct fields")
        void restaurantType_returnsRestaurantUserDetails() {
            when(claims.get(JwtPrincipalConverter.CLAIM_USER_TYPE, String.class))
                    .thenReturn(JwtPrincipalConverter.USER_TYPE_RESTAURANT);
            when(claims.get(JwtPrincipalConverter.CLAIM_USER_ID, Long.class)).thenReturn(7L);
            when(claims.get(JwtPrincipalConverter.CLAIM_TENANT_ID, String.class)).thenReturn("tenant-2");
            when(claims.get(JwtPrincipalConverter.CLAIM_USERNAME, String.class)).thenReturn("kitchen01");
            when(claims.get(JwtPrincipalConverter.CLAIM_ROLE, String.class)).thenReturn("KITCHEN");

            UserDetails result = converter.toPrincipal(claims);

            assertThat(result).isInstanceOf(RestaurantUserDetails.class);
            RestaurantUserDetails r = (RestaurantUserDetails) result;
            assertThat(r.getId()).isEqualTo(7L);
            assertThat(r.getTenantId()).isEqualTo("tenant-2");
            assertThat(r.getPlainUsername()).isEqualTo("kitchen01");
            assertThat(r.getRole()).isEqualTo(UserRole.KITCHEN);
        }

        @Test
        @DisplayName("unknown userType throws JwtException")
        void unknownUserType_throwsJwtException() {
            when(claims.get(JwtPrincipalConverter.CLAIM_USER_TYPE, String.class)).thenReturn("unknown");

            assertThatThrownBy(() -> converter.toPrincipal(claims))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("restaurant type with invalid role name throws JwtException")
        void restaurantType_invalidRole_throwsJwtException() {
            when(claims.get(JwtPrincipalConverter.CLAIM_USER_TYPE, String.class))
                    .thenReturn(JwtPrincipalConverter.USER_TYPE_RESTAURANT);
            when(claims.get(JwtPrincipalConverter.CLAIM_USER_ID, Long.class)).thenReturn(1L);
            when(claims.get(JwtPrincipalConverter.CLAIM_TENANT_ID, String.class)).thenReturn("t");
            when(claims.get(JwtPrincipalConverter.CLAIM_USERNAME, String.class)).thenReturn("u");
            when(claims.get(JwtPrincipalConverter.CLAIM_ROLE, String.class)).thenReturn("INVALID_ROLE");

            assertThatThrownBy(() -> converter.toPrincipal(claims))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("platform type with missing userId throws JwtException")
        void platformType_missingUserId_throwsJwtException() {
            when(claims.get(JwtPrincipalConverter.CLAIM_USER_TYPE, String.class))
                    .thenReturn(JwtPrincipalConverter.USER_TYPE_PLATFORM);
            when(claims.get(JwtPrincipalConverter.CLAIM_USER_ID, Long.class)).thenReturn(null);
            when(claims.get(JwtPrincipalConverter.CLAIM_USERNAME, String.class)).thenReturn("admin");

            assertThatThrownBy(() -> converter.toPrincipal(claims))
                    .isInstanceOf(JwtException.class);
        }
    }
}
