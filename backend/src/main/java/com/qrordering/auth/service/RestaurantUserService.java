package com.qrordering.auth.service;

import com.qrordering.auth.converter.RestaurantUserConverter;
import com.qrordering.auth.dto.request.CreateRestaurantUserRequest;
import com.qrordering.auth.dto.response.RestaurantUserResponse;
import com.qrordering.auth.entity.RestaurantUser;
import com.qrordering.auth.enums.UserRole;
import com.qrordering.auth.repository.RestaurantUserRepository;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.security.RestaurantUserDetails;
import com.qrordering.common.exception.BusinessException;
import com.qrordering.common.exception.ResourceNotFoundException;
import com.qrordering.restaurant.entity.Restaurant;
import com.qrordering.restaurant.enums.RestaurantStatus;
import com.qrordering.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for restaurant staff (user) management.
 * Only RESTAURANT_ADMIN of the same tenant can create users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantUserService {

    private final RestaurantUserRepository restaurantUserRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantUserConverter restaurantUserConverter;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new staff user for the given restaurant.
     * Caller: PLATFORM_ADMIN (any tenant) or RESTAURANT_ADMIN of that tenant.
     */
    @Transactional
    public RestaurantUserResponse createUser(String tenantId, CreateRestaurantUserRequest request) {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof PlatformAdminDetails) {
            // Platform admin can create user for any restaurant
        } else if (principal instanceof RestaurantUserDetails currentUser) {
            if (!currentUser.getTenantId().equals(tenantId)) {
                throw new AccessDeniedException("Cannot create users for another restaurant");
            }
            if (currentUser.getRole() != UserRole.RESTAURANT_ADMIN) {
                throw new AccessDeniedException("Only restaurant admin can create staff");
            }
        } else {
            throw new AccessDeniedException("Authentication required");
        }

        Restaurant restaurant = restaurantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", tenantId));
        if (restaurant.getStatus() != RestaurantStatus.ACTIVE) {
            throw new BusinessException("Restaurant is not active; cannot add staff");
        }

        if (restaurantUserRepository.existsByTenantIdAndUsername(tenantId, request.getUsername())) {
            throw new BusinessException("Username already exists in this restaurant");
        }

        UserRole role = UserRole.fromCode(request.getRole());
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        RestaurantUser user = RestaurantUser.builder()
                .tenantId(tenantId)
                .username(request.getUsername())
                .password(encodedPassword)
                .role(role)
                .email(request.getEmail())
                .build();

        RestaurantUser saved = restaurantUserRepository.save(user);
        log.info("Created staff user id={} username={} in tenant={}", saved.getId(), saved.getUsername(), tenantId);

        return restaurantUserConverter.toResponse(saved);
    }
}
