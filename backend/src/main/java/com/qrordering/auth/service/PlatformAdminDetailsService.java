package com.qrordering.auth.service;

import com.qrordering.auth.entity.PlatformAdmin;
import com.qrordering.auth.repository.PlatformAdminRepository;
import com.qrordering.auth.security.PlatformAdminDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads platform admin by username. Used by {@link AuthUserDetailsService} when tenant is PLATFORM.
 */
@Service
@RequiredArgsConstructor
public class PlatformAdminDetailsService {

    private final PlatformAdminRepository platformAdminRepository;

    public UserDetails loadPlatformAdmin(String plainUsername) {
        PlatformAdmin admin = platformAdminRepository.findByUsername(plainUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Platform admin not found: " + plainUsername));
        return new PlatformAdminDetails(admin.getId(), admin.getUsername(), admin.getPassword());
    }
}
