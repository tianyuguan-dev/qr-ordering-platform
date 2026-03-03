package com.qrordering.restaurant.controller;

import com.qrordering.restaurant.dto.request.CreateRestaurantRequest;
import com.qrordering.restaurant.dto.request.UpdateRestaurantRequest;
import com.qrordering.restaurant.dto.response.RestaurantResponse;
import com.qrordering.restaurant.enums.RestaurantStatus;
import com.qrordering.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Restaurant Management Controller
 *
 * REST API endpoints for restaurant (tenant) management
 */
@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurant Management", description = "APIs for managing restaurants (tenants)")
public class RestaurantController {

    private final RestaurantService restaurantService;

    /**
     * Create a new restaurant
     */
    @PostMapping
    @Operation(summary = "Create restaurant", description = "Create a new restaurant in the system")
    public ResponseEntity<RestaurantResponse> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest request) {
        RestaurantResponse restaurant = restaurantService.createRestaurant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(restaurant);
    }

    /**
     * Get restaurant by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get restaurant", description = "Retrieve restaurant details by ID")
    public ResponseEntity<RestaurantResponse> getRestaurant(
            @Parameter(description = "Restaurant ID", example = "REST_20240304_00001")
            @PathVariable String id) {
        RestaurantResponse restaurant = restaurantService.getRestaurant(id);
        return ResponseEntity.ok(restaurant);
    }

    /**
     * Get all restaurants with pagination
     */
    @GetMapping
    @Operation(summary = "List restaurants", description = "Get all restaurants with pagination")
    public ResponseEntity<Page<RestaurantResponse>> getAllRestaurants(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<RestaurantResponse> restaurants = restaurantService.getAllRestaurants(pageable);
        return ResponseEntity.ok(restaurants);
    }

    /**
     * Get restaurants by status
     */
    @GetMapping("/status/{statusCode}")
    @Operation(summary = "List restaurants by status", description = "Get restaurants filtered by status code with pagination")
    public ResponseEntity<Page<RestaurantResponse>> getRestaurantsByStatus(
            @Parameter(description = "Restaurant status code (1=ACTIVE, 2=SUSPENDED, 3=INACTIVE)", example = "1")
            @PathVariable Integer statusCode,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        RestaurantStatus status = RestaurantStatus.fromCode(statusCode);
        Page<RestaurantResponse> restaurants = restaurantService.getRestaurantsByStatus(status, pageable);
        return ResponseEntity.ok(restaurants);
    }

    /**
     * Update restaurant information
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update restaurant", description = "Update restaurant information")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            @Parameter(description = "Restaurant ID", example = "REST_20240304_00001")
            @PathVariable String id,
            @Valid @RequestBody UpdateRestaurantRequest request) {
        RestaurantResponse restaurant = restaurantService.updateRestaurant(id, request);
        return ResponseEntity.ok(restaurant);
    }

    /**
     * Delete restaurant
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete restaurant", description = "Delete a restaurant from the system")
    public ResponseEntity<Void> deleteRestaurant(
            @Parameter(description = "Restaurant ID", example = "REST_20240304_00001")
            @PathVariable String id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.noContent().build();
    }
}
