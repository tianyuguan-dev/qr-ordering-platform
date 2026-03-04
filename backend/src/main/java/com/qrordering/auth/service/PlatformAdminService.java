package com.qrordering.auth.service;

import com.qrordering.auth.dto.request.CreatePlatformAdminRequest;
import com.qrordering.auth.dto.request.ResetPlatformAdminPasswordRequest;
import com.qrordering.auth.dto.request.UpdatePlatformAdminRequest;
import com.qrordering.auth.dto.response.PlatformAdminResponse;
import com.qrordering.auth.entity.PlatformAdmin;
import com.qrordering.auth.repository.PlatformAdminRepository;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.common.exception.BusinessException;
import com.qrordering.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for platform administrator management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminService {

    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;

    public List<PlatformAdminResponse> listAll() {
        return platformAdminRepository.findAll().stream()
                .map(a -> PlatformAdminResponse.builder()
                        .id(a.getId())
                        .username(a.getUsername())
                        .email(a.getEmail())
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Create a new platform admin. Only an existing platform admin may call this.
     */
    @Transactional
    public PlatformAdmin createPlatformAdmin(CreatePlatformAdminRequest request) {
        if (platformAdminRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("Username already exists for platform admin");
        }
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        PlatformAdmin admin = PlatformAdmin.builder()
                .username(request.getUsername())
                .password(encodedPassword)
                .email(request.getEmail())
                .build();
        admin = platformAdminRepository.save(admin);
        log.info("Created platform admin: username={}", admin.getUsername());
        return admin;
    }

    /**
     * Update platform admin (email only). Cannot change username.
     */
    @Transactional
    public PlatformAdminResponse update(Long id, UpdatePlatformAdminRequest request) {
        PlatformAdmin admin = platformAdminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Platform admin", String.valueOf(id)));
        if (request.getEmail() != null) {
            admin.setEmail(request.getEmail().trim().isEmpty() ? null : request.getEmail().trim());
        }
        platformAdminRepository.save(admin);
        log.info("Updated platform admin id={}", id);
        return toResponse(admin);
    }

    /**
     * Delete platform admin. Cannot delete self. Cannot delete last admin.
     */
    @Transactional
    public void delete(Long id) {
        Long currentId = currentPlatformAdminId();
        if (currentId != null && currentId.equals(id)) {
            throw new BusinessException("Cannot delete yourself");
        }
        PlatformAdmin admin = platformAdminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Platform admin", String.valueOf(id)));
        if (platformAdminRepository.count() <= 1) {
            throw new BusinessException("Cannot delete the last platform admin");
        }
        platformAdminRepository.delete(admin);
        log.info("Deleted platform admin id={}", id);
    }

    /**
     * Reset another platform admin's password. Cannot reset own (use change-password).
     */
    @Transactional
    public void resetPassword(Long id, ResetPlatformAdminPasswordRequest request) {
        Long currentId = currentPlatformAdminId();
        if (currentId != null && currentId.equals(id)) {
            throw new BusinessException("Use change-password to change your own password");
        }
        PlatformAdmin admin = platformAdminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Platform admin", String.valueOf(id)));
        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        platformAdminRepository.save(admin);
        log.info("Reset password for platform admin id={}", id);
    }

    private Long currentPlatformAdminId() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof PlatformAdminDetails p ? p.getId() : null;
    }

    private static PlatformAdminResponse toResponse(PlatformAdmin a) {
        return PlatformAdminResponse.builder()
                .id(a.getId())
                .username(a.getUsername())
                .email(a.getEmail())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
