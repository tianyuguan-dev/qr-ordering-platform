package com.qrordering.auth.service;

import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.security.RestaurantUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Delegating UserDetailsService: parses "tenantId|username" and delegates to
 * {@link PlatformAdminDetailsService} (when tenant is PLATFORM) or
 * {@link RestaurantUserDetailsService} (otherwise).
 */
@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final PlatformAdminDetailsService platformAdminDetailsService;
    private final RestaurantUserDetailsService restaurantUserDetailsService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String tenantId;
        String plainUsername;
        try {
            int idx = username.indexOf(RestaurantUserDetails.USERNAME_DELIMITER);
            if (idx <= 0 || idx == username.length() - 1) {
                throw new UsernameNotFoundException("Invalid username format: expected tenantId|username");
            }
            tenantId = username.substring(0, idx);
            plainUsername = username.substring(idx + 1);
        } catch (Exception e) {
            if (e instanceof UsernameNotFoundException) {
                throw (UsernameNotFoundException) e;
            }
            throw new UsernameNotFoundException("Invalid username format: expected tenantId|username", e);
        }

        if (PlatformAdminDetails.PLATFORM_TENANT_ID.equals(tenantId)) {
            return platformAdminDetailsService.loadPlatformAdmin(plainUsername);
        }
        return restaurantUserDetailsService.loadRestaurantUser(tenantId, plainUsername);
    }
}
