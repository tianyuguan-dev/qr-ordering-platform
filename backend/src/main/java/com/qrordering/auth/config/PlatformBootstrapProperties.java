package com.qrordering.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional bootstrap for first platform admin when none exists.
 * Values are bound from application.yml (platform.bootstrap.*). When enabled=true,
 * username and password must be set in config; no in-code defaults.
 */
@ConfigurationProperties(prefix = "platform.bootstrap")
public class PlatformBootstrapProperties {

    private boolean enabled = true;
    /** From platform.bootstrap.username in yml; required when enabled. */
    private String username;
    /** From platform.bootstrap.password in yml; required when enabled. */
    private String password;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
