package com.qrordering.auth.service;

import com.qrordering.auth.dto.request.CreatePlatformAdminRequest;
import com.qrordering.auth.entity.PlatformAdmin;
import com.qrordering.auth.repository.PlatformAdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qrordering.auth.dto.response.PlatformAdminResponse;
import com.qrordering.common.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;

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
}
