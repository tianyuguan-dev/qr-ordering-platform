package com.qrordering.auth.service;

import com.qrordering.auth.converter.RestaurantUserConverter;
import com.qrordering.auth.dto.request.CreateRestaurantUserRequest;
import com.qrordering.auth.dto.request.ResetRestaurantUserPasswordRequest;
import com.qrordering.auth.dto.request.UpdateRestaurantUserRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * List staff users of a restaurant with pagination.
     * PLATFORM_ADMIN can list any restaurant; RESTAURANT_ADMIN can list only their own tenant.
     */
    public Page<RestaurantUserResponse> listByTenantId(String tenantId, Pageable pageable) {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof PlatformAdminDetails) {
            // Platform admin can list any restaurant's users
        } else if (principal instanceof RestaurantUserDetails currentUser) {
            if (!currentUser.getTenantId().equals(tenantId)) {
                throw new AccessDeniedException("Cannot list users of another restaurant");
            }
        } else {
            throw new AccessDeniedException("Authentication required");
        }

        return restaurantUserRepository.findByTenantId(tenantId, pageable)
                .map(restaurantUserConverter::toResponse);
    }

    /**
     * Update staff user (email, role). Caller: PLATFORM_ADMIN or RESTAURANT_ADMIN of that tenant.
     */
    @Transactional
    public RestaurantUserResponse updateUser(String tenantId, Long userId, UpdateRestaurantUserRequest request) {
        checkTenantAccess(tenantId, true);
        RestaurantUser user = getAndVerifyUserInTenant(userId, tenantId);
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim().isEmpty() ? null : request.getEmail().trim());
        }
        if (request.getRole() != null) {
            user.setRole(UserRole.fromCode(request.getRole()));
        }
        RestaurantUser saved = restaurantUserRepository.save(user);
        log.info("Updated staff user id={} in tenant={}", userId, tenantId);
        return restaurantUserConverter.toResponse(saved);
    }

    /**
     * Delete staff user. Cannot delete self. Caller: PLATFORM_ADMIN or RESTAURANT_ADMIN of that tenant.
     */
    @Transactional
    public void deleteUser(String tenantId, Long userId) {
        checkTenantAccess(tenantId, true);
        RestaurantUser user = getAndVerifyUserInTenant(userId, tenantId);
        if (currentRestaurantUserId() != null && currentRestaurantUserId().equals(userId)) {
            throw new BusinessException("Cannot delete yourself");
        }
        restaurantUserRepository.delete(user);
        log.info("Deleted staff user id={} from tenant={}", userId, tenantId);
    }

    /**
     * Reset staff password. Cannot reset own (use change-password). Caller: PLATFORM_ADMIN or RESTAURANT_ADMIN of that tenant.
     */
    @Transactional
    public void resetPassword(String tenantId, Long userId, ResetRestaurantUserPasswordRequest request) {
        checkTenantAccess(tenantId, true);
        if (currentRestaurantUserId() != null && currentRestaurantUserId().equals(userId)) {
            throw new BusinessException("Use change-password to change your own password");
        }
        RestaurantUser user = getAndVerifyUserInTenant(userId, tenantId);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        restaurantUserRepository.save(user);
        log.info("Reset password for staff user id={} in tenant={}", userId, tenantId);
    }

    private void checkTenantAccess(String tenantId, boolean requireAdminForRestaurant) {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof PlatformAdminDetails) {
            return;
        }
        if (principal instanceof RestaurantUserDetails currentUser) {
            if (!currentUser.getTenantId().equals(tenantId)) {
                throw new AccessDeniedException("Cannot manage users of another restaurant");
            }
            if (requireAdminForRestaurant && currentUser.getRole() != UserRole.RESTAURANT_ADMIN) {
                throw new AccessDeniedException("Only restaurant admin can manage staff");
            }
            return;
        }
        throw new AccessDeniedException("Authentication required");
    }

    private Long currentRestaurantUserId() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof RestaurantUserDetails r ? r.getId() : null;
    }

    private RestaurantUser getAndVerifyUserInTenant(Long userId, String tenantId) {
        RestaurantUser user = restaurantUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant user", String.valueOf(userId)));
        if (!user.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Restaurant user", String.valueOf(userId));
        }
        return user;
    }
}
