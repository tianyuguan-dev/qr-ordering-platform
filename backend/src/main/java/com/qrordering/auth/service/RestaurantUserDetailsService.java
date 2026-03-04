package com.qrordering.auth.service;

import com.qrordering.auth.entity.RestaurantUser;
import com.qrordering.auth.repository.RestaurantUserRepository;
import com.qrordering.auth.security.RestaurantUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads restaurant staff by tenantId and username. Used by {@link AuthUserDetailsService} for non-PLATFORM tenants.
 */
@Service
@RequiredArgsConstructor
public class RestaurantUserDetailsService {

    private final RestaurantUserRepository restaurantUserRepository;

    public UserDetails loadRestaurantUser(String tenantId, String plainUsername) {
        RestaurantUser user = restaurantUserRepository.findByTenantIdAndUsername(tenantId, plainUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + plainUsername + " in tenant " + tenantId));
        return new RestaurantUserDetails(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole()
        );
    }
}
