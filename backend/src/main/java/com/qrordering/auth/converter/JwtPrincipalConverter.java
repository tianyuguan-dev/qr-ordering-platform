package com.qrordering.auth.converter;

import com.qrordering.auth.enums.UserRole;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.security.RestaurantUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Converts between UserDetails and JWT claim values (for building/parsing token payload).
 */
@Component
public class JwtPrincipalConverter {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USER_TYPE = "userType";
    public static final String USER_TYPE_PLATFORM = "platform";
    public static final String USER_TYPE_RESTAURANT = "restaurant";

    /**
     * Extract claim values from principal for JWT building.
     */
    public JwtClaimValues toClaimValues(UserDetails userDetails) {
        if (userDetails instanceof PlatformAdminDetails p) {
            return new JwtClaimValues(p.getId(), PlatformAdminDetails.PLATFORM_TENANT_ID,
                    p.getPlainUsername(), null, USER_TYPE_PLATFORM);
        }
        if (userDetails instanceof RestaurantUserDetails r) {
            return new JwtClaimValues(r.getId(), r.getTenantId(), r.getPlainUsername(),
                    r.getRole().name(), USER_TYPE_RESTAURANT);
        }
        throw new JwtException("Unsupported principal type for JWT: " + userDetails.getClass().getSimpleName());
    }

    /**
     * Build UserDetails from JWT claims (after validation).
     */
    public UserDetails toPrincipal(Claims claims) {
        String userType = claims.get(CLAIM_USER_TYPE, String.class);
        if (USER_TYPE_PLATFORM.equals(userType)) {
            Long userId = claims.get(CLAIM_USER_ID, Long.class);
            String username = claims.get(CLAIM_USERNAME, String.class);
            if (userId == null || username == null) {
                throw new JwtException("Missing claims for platform admin JWT");
            }
            return new PlatformAdminDetails(userId, username, "");
        }
        if (USER_TYPE_RESTAURANT.equals(userType)) {
            Long userId = claims.get(CLAIM_USER_ID, Long.class);
            String tenantId = claims.get(CLAIM_TENANT_ID, String.class);
            String username = claims.get(CLAIM_USERNAME, String.class);
            String roleName = claims.get(CLAIM_ROLE, String.class);
            if (userId == null || tenantId == null || username == null || roleName == null) {
                throw new JwtException("Missing required claims in restaurant JWT");
            }
            UserRole role;
            try {
                role = UserRole.valueOf(roleName);
            } catch (IllegalArgumentException e) {
                throw new JwtException("Invalid role in JWT: " + roleName, e);
            }
            return new RestaurantUserDetails(userId, tenantId, username, "", role);
        }
        throw new JwtException("Unknown or missing userType in JWT");
    }

    /**
     * Claim values to put in JWT payload. role is null for platform admin.
     */
    public record JwtClaimValues(Long userId, String tenantId, String username, String role, String userType) {}
}
