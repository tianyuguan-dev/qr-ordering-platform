package com.qrordering.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT configuration. Values are bound from application.yml (jwt.secret, jwt.expiration).
 * Both must be set in config; no in-code defaults.
 */
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {

    /** Signing secret (from jwt.secret in yml). HS256 requires at least 256 bits. */
    @NotBlank(message = "jwt.secret must be set in application.yml or environment")
    private String secret;
    /** Token validity in milliseconds (from jwt.expiration in yml). */
    @Positive(message = "jwt.expiration must be positive")
    private long expiration;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}
