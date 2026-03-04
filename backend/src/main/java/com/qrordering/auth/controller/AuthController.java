package com.qrordering.auth.controller;

import com.qrordering.auth.converter.AuthConverter;
import com.qrordering.auth.dto.request.ChangePasswordRequest;
import com.qrordering.auth.dto.request.LoginRequest;
import com.qrordering.auth.dto.response.LoginResponse;
import com.qrordering.auth.security.RestaurantUserDetails;
import com.qrordering.auth.service.ChangePasswordService;
import com.qrordering.auth.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints (login for restaurant staff and platform admin).
 * Platform admin: tenantId=PLATFORM. Restaurant: tenantId=restaurant id.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and auth-related APIs")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthConverter authConverter;
    private final ChangePasswordService changePasswordService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Restaurant: tenantId=restaurant id. Platform admin: tenantId=PLATFORM.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String principal = request.getTenantId() + RestaurantUserDetails.USERNAME_DELIMITER + request.getUsername();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(principal, request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(userDetails);
        LoginResponse response = authConverter.toLoginResponse(userDetails, accessToken);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Current user profile", description = "Returns the authenticated user's profile (no token).")
    public ResponseEntity<LoginResponse> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        LoginResponse response = authConverter.toLoginResponse(principal, null);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password", description = "Change the authenticated user's password.")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        changePasswordService.changePassword(principal, request);
        return ResponseEntity.noContent().build();
    }
}
