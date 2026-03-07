package com.qrordering.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Controls whether demo seed data (restaurant, staff, menu, tables) is created on startup.
 * Bound from application.yml (demo.data.*).
 */
@ConfigurationProperties(prefix = "demo.data")
public class DemoDataProperties {

    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
