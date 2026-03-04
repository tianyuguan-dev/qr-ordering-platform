package com.qrordering.auth.controller;

import com.qrordering.auth.dto.request.CreatePlatformAdminRequest;
import com.qrordering.auth.dto.request.ResetPlatformAdminPasswordRequest;
import com.qrordering.auth.dto.request.UpdatePlatformAdminRequest;
import com.qrordering.auth.dto.response.PlatformAdminResponse;
import com.qrordering.auth.entity.PlatformAdmin;
import com.qrordering.auth.service.PlatformAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Platform administrator management. Only existing platform admins can create new ones.
 */
@RestController
@RequestMapping("/platform-admins")
@RequiredArgsConstructor
@Tag(name = "Platform Admin", description = "APIs for managing platform administrators")
public class PlatformAdminController {

    private final PlatformAdminService platformAdminService;

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List platform admins", description = "List all platform administrators.")
    public ResponseEntity<List<PlatformAdminResponse>> listPlatformAdmins() {
        List<PlatformAdminResponse> list = platformAdminService.listAll();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create platform admin", description = "Create a new platform administrator (only existing platform admin).")
    public ResponseEntity<PlatformAdminResponse> createPlatformAdmin(
            @Valid @RequestBody CreatePlatformAdminRequest request) {
        PlatformAdmin admin = platformAdminService.createPlatformAdmin(request);
        PlatformAdminResponse response = PlatformAdminResponse.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .email(admin.getEmail())
                .createdAt(admin.getCreatedAt())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update platform admin", description = "Update email (username immutable)")
    public ResponseEntity<PlatformAdminResponse> updatePlatformAdmin(
            @Parameter(description = "Platform admin ID") @PathVariable Long id,
            @Valid @RequestBody UpdatePlatformAdminRequest request) {
        return ResponseEntity.ok(platformAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Delete platform admin", description = "Cannot delete self or last admin")
    public ResponseEntity<Void> deletePlatformAdmin(
            @Parameter(description = "Platform admin ID") @PathVariable Long id) {
        platformAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Reset password", description = "Reset another admin's password (not own)")
    public ResponseEntity<Void> resetPassword(
            @Parameter(description = "Platform admin ID") @PathVariable Long id,
            @Valid @RequestBody ResetPlatformAdminPasswordRequest request) {
        platformAdminService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
