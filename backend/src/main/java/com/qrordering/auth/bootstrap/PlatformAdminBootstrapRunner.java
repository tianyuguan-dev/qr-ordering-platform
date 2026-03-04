package com.qrordering.auth.bootstrap;

import com.qrordering.auth.entity.PlatformAdmin;
import com.qrordering.auth.repository.PlatformAdminRepository;
import com.qrordering.auth.config.PlatformBootstrapProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first platform admin if none exist (when platform.bootstrap.enabled=true).
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminBootstrapRunner implements ApplicationRunner {

    private final PlatformAdminRepository platformAdminRepository;
    private final PlatformBootstrapProperties bootstrapProperties;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapProperties.isEnabled()) {
            return;
        }
        if (platformAdminRepository.count() > 0) {
            return;
        }
        String username = bootstrapProperties.getUsername();
        String password = bootstrapProperties.getPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("platform.bootstrap.username and password must be set in config when bootstrap is enabled; skipping.");
            return;
        }
        if (platformAdminRepository.findByUsername(username).isPresent()) {
            return;
        }
        String encodedPassword = passwordEncoder.encode(password);
        PlatformAdmin admin = PlatformAdmin.builder()
                .username(username)
                .password(encodedPassword)
                .build();
        platformAdminRepository.save(admin);
        log.info("Bootstrapped first platform admin: username={}. Change password in production.", username);
    }
}
