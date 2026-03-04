package com.qrordering.auth.security;

import com.qrordering.auth.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security UserDetails for restaurant staff.
 * Wraps {@link com.qrordering.auth.entity.RestaurantUser} and exposes tenantId/role for authz and JWT.
 */
@Getter
public class RestaurantUserDetails implements UserDetails {

    private final Long id;
    private final String tenantId;
    private final String username;
    private final String password;
    private final UserRole role;

    /**
     * Identity format used when calling loadUserByUsername: "tenantId|username".
     * TenantId and username must not contain this delimiter.
     */
    public static final String USERNAME_DELIMITER = "|";

    public RestaurantUserDetails(Long id, String tenantId, String username, String password, UserRole role) {
        this.id = id;
        this.tenantId = tenantId;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    /**
     * Username for Spring Security is the composite "tenantId|username" so that
     * the same identity is used in the Authentication principal.
     */
    @Override
    public String getUsername() {
        return tenantId + USERNAME_DELIMITER + username;
    }

    /**
     * Plain username (without tenant prefix). Use this in JWT or API responses.
     */
    public String getPlainUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
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
