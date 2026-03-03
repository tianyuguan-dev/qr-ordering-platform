package com.qrordering.restaurant.repository;

import com.qrordering.restaurant.entity.Restaurant;
import com.qrordering.restaurant.enums.RestaurantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Restaurant Repository
 *
 * Data access layer for Restaurant entity.
 * Note: @Repository annotation is not required for Spring Data JPA interfaces.
 */
public interface RestaurantRepository extends JpaRepository<Restaurant, String> {

    /**
     * Find restaurant by name (case-insensitive)
     */
    Optional<Restaurant> findByNameIgnoreCase(String name);

    /**
     * Find all restaurants by status with pagination
     */
    Page<Restaurant> findByStatus(RestaurantStatus status, Pageable pageable);

    /**
     * Check if restaurant with given name exists
     */
    boolean existsByNameIgnoreCase(String name);
}
