package com.qrordering.auth.repository;

import com.qrordering.auth.entity.RestaurantUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Restaurant User Repository
 *
 * Data access for restaurant staff (tenant-scoped users).
 * Used by login (findByTenantIdAndUsername) and staff management.
 */
public interface RestaurantUserRepository extends JpaRepository<RestaurantUser, Long> {

    /**
     * Find user by tenant and username (unique within tenant).
     * Used for authentication: load user by login identity.
     */
    Optional<RestaurantUser> findByTenantIdAndUsername(String tenantId, String username);

    /**
     * Check if username already exists in the given restaurant.
     * Used when creating new staff to avoid duplicate username.
     */
    boolean existsByTenantIdAndUsername(String tenantId, String username);

    /**
     * List all staff of a restaurant with pagination.
     */
    Page<RestaurantUser> findByTenantId(String tenantId, Pageable pageable);
}
