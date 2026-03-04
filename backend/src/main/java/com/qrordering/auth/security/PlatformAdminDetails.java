package com.qrordering.auth.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * UserDetails for platform administrator (no tenant).
 * Identity format: PLATFORM|username to align with login.
 */
@Getter
public class PlatformAdminDetails implements UserDetails {

    public static final String PLATFORM_TENANT_ID = "PLATFORM";

    private final Long id;
    private final String plainUsername;
    private final String password;

    public PlatformAdminDetails(Long id, String plainUsername, String password) {
        this.id = id;
        this.plainUsername = plainUsername;
        this.password = password;
    }

    @Override
    public String getUsername() {
        return PLATFORM_TENANT_ID + RestaurantUserDetails.USERNAME_DELIMITER + plainUsername;
    }

    public String getPlainUsername() {
        return plainUsername;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
