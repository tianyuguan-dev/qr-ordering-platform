package com.qrordering.auth.service;

import com.qrordering.auth.config.JwtProperties;
import com.qrordering.auth.converter.JwtPrincipalConverter;
import com.qrordering.auth.enums.UserRole;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.security.RestaurantUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtService — no Spring context, no mocks.
 * Directly wires JwtProperties + JwtPrincipalConverter + JwtService.
 */
@DisplayName("JwtService")
class JwtServiceTest {

    // HS256 requires at least 256 bits (32 bytes); use 64-char string for safety
    private static final String SECRET =
            "test-secret-key-for-unit-tests-that-is-long-enough-for-hmac-sha256!!";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setExpiration(EXPIRATION_MS);
        jwtService = new JwtService(props, new JwtPrincipalConverter());
    }

    // ------------------------------------------------------------------ generate + validate round-trips

    @Nested
    @DisplayName("generateToken + validateAndGetClaims round-trip")
    class RoundTrip {

        @Test
        @DisplayName("restaurant user: claims contain correct tenantId, username, role, userType")
        void restaurantUser_claimsAreCorrect() {
            RestaurantUserDetails user = new RestaurantUserDetails(
                    10L, "tenant-abc", "waiter01", "", UserRole.WAITER);

            String token = jwtService.generateToken(user);
            Claims claims = jwtService.validateAndGetClaims(token);

            assertThat(claims.get(JwtPrincipalConverter.CLAIM_TENANT_ID, String.class))
                    .isEqualTo("tenant-abc");
            assertThat(claims.get(JwtPrincipalConverter.CLAIM_USERNAME, String.class))
                    .isEqualTo("waiter01");
            assertThat(claims.get(JwtPrincipalConverter.CLAIM_ROLE, String.class))
                    .isEqualTo("WAITER");
            assertThat(claims.get(JwtPrincipalConverter.CLAIM_USER_TYPE, String.class))
                    .isEqualTo(JwtPrincipalConverter.USER_TYPE_RESTAURANT);
            assertThat(claims.get(JwtPrincipalConverter.CLAIM_USER_ID, Long.class))
                    .isEqualTo(10L);
        }

        @Test
        @DisplayName("platform admin: claims contain PLATFORM tenantId and null role")
        void platformAdmin_claimsAreCorrect() {
            PlatformAdminDetails admin = new PlatformAdminDetails(99L, "sysadmin", "");

            String token = jwtService.generateToken(admin);
            Claims claims = jwtService.validateAndGetClaims(token);

            assertThat(claims.get(JwtPrincipalConverter.CLAIM_TENANT_ID, String.class))
                    .isEqualTo(PlatformAdminDetails.PLATFORM_TENANT_ID);
            assertThat(claims.get(JwtPrincipalConverter.CLAIM_USER_TYPE, String.class))
                    .isEqualTo(JwtPrincipalConverter.USER_TYPE_PLATFORM);
            assertThat(claims.get(JwtPrincipalConverter.CLAIM_ROLE, String.class)).isNull();
        }
    }

    // ------------------------------------------------------------------ buildPrincipalFromClaims

    @Nested
    @DisplayName("buildPrincipalFromClaims")
    class BuildPrincipal {

        @Test
        @DisplayName("restaurant user: principal has correct type, tenantId, role")
        void restaurantUser_principalIsRestaurantUserDetails() {
            RestaurantUserDetails original = new RestaurantUserDetails(
                    5L, "tenant-1", "admin01", "", UserRole.RESTAURANT_ADMIN);

            String token = jwtService.generateToken(original);
            Claims claims = jwtService.validateAndGetClaims(token);
            UserDetails principal = jwtService.buildPrincipalFromClaims(claims);

            assertThat(principal).isInstanceOf(RestaurantUserDetails.class);
            RestaurantUserDetails r = (RestaurantUserDetails) principal;
            assertThat(r.getId()).isEqualTo(5L);
            assertThat(r.getTenantId()).isEqualTo("tenant-1");
            assertThat(r.getPlainUsername()).isEqualTo("admin01");
            assertThat(r.getRole()).isEqualTo(UserRole.RESTAURANT_ADMIN);
        }

        @Test
        @DisplayName("platform admin: principal is PlatformAdminDetails")
        void platformAdmin_principalIsPlatformAdminDetails() {
            PlatformAdminDetails original = new PlatformAdminDetails(7L, "rootadmin", "");

            String token = jwtService.generateToken(original);
            Claims claims = jwtService.validateAndGetClaims(token);
            UserDetails principal = jwtService.buildPrincipalFromClaims(claims);

            assertThat(principal).isInstanceOf(PlatformAdminDetails.class);
            PlatformAdminDetails p = (PlatformAdminDetails) principal;
            assertThat(p.getId()).isEqualTo(7L);
            assertThat(p.getPlainUsername()).isEqualTo("rootadmin");
        }

        @Test
        @DisplayName("all restaurant roles survive round-trip without data loss")
        void allRoles_roundTripPreservesRole() {
            for (UserRole role : UserRole.values()) {
                RestaurantUserDetails user = new RestaurantUserDetails(1L, "t", "u", "", role);
                String token = jwtService.generateToken(user);
                Claims claims = jwtService.validateAndGetClaims(token);
                RestaurantUserDetails principal = (RestaurantUserDetails) jwtService.buildPrincipalFromClaims(claims);
                assertThat(principal.getRole()).isEqualTo(role);
            }
        }
    }

    // ------------------------------------------------------------------ validateAndGetClaims error cases

    @Nested
    @DisplayName("validateAndGetClaims errors")
    class ValidationErrors {

        @Test
        @DisplayName("invalid token string throws JwtException")
        void invalidToken_throwsJwtException() {
            assertThatThrownBy(() -> jwtService.validateAndGetClaims("not.a.jwt"))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("tampered token throws JwtException")
        void tamperedToken_throwsJwtException() {
            RestaurantUserDetails user = new RestaurantUserDetails(1L, "t", "u", "", UserRole.WAITER);
            String token = jwtService.generateToken(user);
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";

            assertThatThrownBy(() -> jwtService.validateAndGetClaims(tampered))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("expired token throws ExpiredJwtException")
        void expiredToken_throwsExpiredJwtException() {
            JwtProperties expiredProps = new JwtProperties();
            expiredProps.setSecret(SECRET);
            expiredProps.setExpiration(-100_000L); // expired 100 seconds ago
            JwtService expiredJwtService = new JwtService(expiredProps, new JwtPrincipalConverter());

            RestaurantUserDetails user = new RestaurantUserDetails(1L, "t", "u", "", UserRole.WAITER);
            String token = expiredJwtService.generateToken(user);

            assertThatThrownBy(() -> expiredJwtService.validateAndGetClaims(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }
}
