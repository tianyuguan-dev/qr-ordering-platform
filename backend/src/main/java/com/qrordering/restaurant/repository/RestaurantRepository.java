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
     * Find restaurants by name containing (case-insensitive) with pagination
     */
    Page<Restaurant> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find restaurants by status and name containing (case-insensitive) with pagination
     */
    Page<Restaurant> findByStatusAndNameContainingIgnoreCase(RestaurantStatus status, String name, Pageable pageable);

    /**
     * Check if restaurant with given name exists
     */
    boolean existsByNameIgnoreCase(String name);
}
