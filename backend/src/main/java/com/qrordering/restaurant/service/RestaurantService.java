package com.qrordering.restaurant.service;

import com.qrordering.common.exception.BusinessException;
import com.qrordering.common.exception.ResourceNotFoundException;
import com.qrordering.restaurant.converter.RestaurantConverter;
import com.qrordering.restaurant.dto.request.CreateRestaurantRequest;
import com.qrordering.restaurant.dto.request.UpdateRestaurantRequest;
import com.qrordering.restaurant.dto.response.RestaurantResponse;
import com.qrordering.restaurant.entity.Restaurant;
import com.qrordering.restaurant.enums.RestaurantStatus;
import com.qrordering.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Restaurant Service
 *
 * Business logic for restaurant management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantConverter restaurantConverter;

    /**
     * Create a new restaurant
     */
    @Transactional
    public RestaurantResponse createRestaurant(CreateRestaurantRequest request) {
        log.info("Creating restaurant: {}", request.getName());

        // Check for duplicate name
        if (restaurantRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Restaurant with name '" + request.getName() + "' already exists");
        }

        // Generate restaurant ID
        String restaurantId = generateRestaurantId();

        // Build restaurant entity
        Restaurant restaurant = Restaurant.builder()
                .id(restaurantId)
                .name(request.getName())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .address(request.getAddress())
                .phone(request.getPhone())
                .status(RestaurantStatus.ACTIVE)
                .build();

        // Save to database
        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant created successfully with ID: {}", saved.getId());

        return restaurantConverter.toResponse(saved);
    }

    /**
     * Get restaurant by ID
     */
    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant(String id) {
        log.debug("Fetching restaurant with ID: {}", id);

        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));

        return restaurantConverter.toResponse(restaurant);
    }

    /**
     * Get all restaurants with pagination
     */
    @Transactional(readOnly = true)
    public Page<RestaurantResponse> getAllRestaurants(Pageable pageable) {
        log.debug("Fetching all restaurants with pagination: {}", pageable);

        return restaurantRepository.findAll(pageable)
                .map(restaurantConverter::toResponse);
    }

    /**
     * Get restaurants by status with pagination
     */
    @Transactional(readOnly = true)
    public Page<RestaurantResponse> getRestaurantsByStatus(RestaurantStatus status, Pageable pageable) {
        log.debug("Fetching restaurants with status {} and pagination: {}", status, pageable);

        return restaurantRepository.findByStatus(status, pageable)
                .map(restaurantConverter::toResponse);
    }

    /**
     * Update restaurant information
     */
    @Transactional
    public RestaurantResponse updateRestaurant(String id, UpdateRestaurantRequest request) {
        log.info("Updating restaurant with ID: {}", id);

        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));

        // Check for duplicate name if name is being changed
        if (request.getName() != null && !request.getName().equals(restaurant.getName())) {
            if (restaurantRepository.existsByNameIgnoreCase(request.getName())) {
                throw new BusinessException("Restaurant with name '" + request.getName() + "' already exists");
            }
            restaurant.setName(request.getName());
        }

        // Update other fields if provided
        if (request.getDescription() != null) {
            restaurant.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            restaurant.setLogoUrl(request.getLogoUrl());
        }
        if (request.getAddress() != null) {
            restaurant.setAddress(request.getAddress());
        }
        if (request.getPhone() != null) {
            restaurant.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            RestaurantStatus status = RestaurantStatus.fromCode(request.getStatus());
            restaurant.setStatus(status);
        }

        Restaurant updated = restaurantRepository.save(restaurant);
        log.info("Restaurant updated successfully: {}", updated.getId());

        return restaurantConverter.toResponse(updated);
    }

    /**
     * Delete restaurant by ID
     */
    @Transactional
    public void deleteRestaurant(String id) {
        log.info("Deleting restaurant with ID: {}", id);

        if (!restaurantRepository.existsById(id)) {
            throw new ResourceNotFoundException("Restaurant", id);
        }

        restaurantRepository.deleteById(id);
        log.info("Restaurant deleted successfully: {}", id);
    }

    /**
     * Generate unique restaurant ID using UUID
     * Format: REST_YYYYMMDD_XXXXXXXXXXXX (12 hex characters from UUID)
     *
     * Note: UUID ensures global uniqueness across application restarts and distributed deployments.
     * Alternative production approaches: database sequences, Snowflake algorithm, or Redis counters.
     */
    private String generateRestaurantId() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return String.format("REST_%s_%s", datePart, uuidPart);
    }
}
