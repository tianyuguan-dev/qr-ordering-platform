package com.qrordering.auth.controller;

import com.qrordering.auth.dto.request.CreateRestaurantUserRequest;
import com.qrordering.auth.dto.response.RestaurantUserResponse;
import com.qrordering.auth.service.RestaurantUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Restaurant staff management. Only restaurant admin of the same tenant can create users.
 */
@RestController
@RequestMapping("/restaurants/{tenantId}/users")
@RequiredArgsConstructor
@Tag(name = "Restaurant Staff", description = "APIs for managing restaurant staff (tenant-scoped)")
public class RestaurantUserController {

    private final RestaurantUserService restaurantUserService;

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_ADMIN') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create staff user", description = "Platform admin or restaurant admin of this tenant can create staff.")
    public ResponseEntity<RestaurantUserResponse> createUser(
            @Parameter(description = "Restaurant (tenant) ID", example = "REST_20240304_ABC123")
            @PathVariable String tenantId,
            @Valid @RequestBody CreateRestaurantUserRequest request) {
        RestaurantUserResponse response = restaurantUserService.createUser(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
