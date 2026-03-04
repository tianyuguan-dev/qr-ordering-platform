package com.qrordering.auth.service;

import com.qrordering.auth.dto.request.ChangePasswordRequest;
import com.qrordering.auth.entity.PlatformAdmin;
import com.qrordering.auth.entity.RestaurantUser;
import com.qrordering.auth.repository.PlatformAdminRepository;
import com.qrordering.auth.repository.RestaurantUserRepository;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.security.RestaurantUserDetails;
import com.qrordering.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changes the current user's password (platform admin or restaurant user).
 */
@Service
@RequiredArgsConstructor
public class ChangePasswordService {

    private final PlatformAdminRepository platformAdminRepository;
    private final RestaurantUserRepository restaurantUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(UserDetails principal, ChangePasswordRequest request) {
        if (principal instanceof PlatformAdminDetails p) {
            PlatformAdmin admin = platformAdminRepository.findById(p.getId())
                    .orElseThrow(() -> new BusinessException("Platform admin not found"));
            if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPassword())) {
                throw new BusinessException("Current password is incorrect");
            }
            admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
            platformAdminRepository.save(admin);
            return;
        }
        if (principal instanceof RestaurantUserDetails r) {
            RestaurantUser user = restaurantUserRepository.findById(r.getId())
                    .orElseThrow(() -> new BusinessException("User not found"));
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new BusinessException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            restaurantUserRepository.save(user);
            return;
        }
        throw new BusinessException("Unsupported user type");
    }
}
