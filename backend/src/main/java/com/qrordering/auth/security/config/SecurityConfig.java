package com.qrordering.auth.security.config;

import com.qrordering.auth.config.JwtProperties;
import com.qrordering.auth.config.PlatformBootstrapProperties;
import com.qrordering.auth.security.Http401EntryPoint;
import com.qrordering.auth.security.Http403AccessDeniedHandler;
import com.qrordering.auth.security.filter.JwtAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration
 *
 * Uses {@link com.qrordering.auth.service.AuthUserDetailsService} for loading
 * users (identity format tenantId|username; delegates to platform or restaurant loader). JWT in Authorization header
 * is validated by {@link JwtAuthenticationFilter}; unauthenticated access to protected
 * paths returns 401 via {@link Http401EntryPoint}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, PlatformBootstrapProperties.class})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Http401EntryPoint http401EntryPoint;
    private final Http403AccessDeniedHandler http403AccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          Http401EntryPoint http401EntryPoint,
                          Http403AccessDeniedHandler http403AccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.http401EntryPoint = http401EntryPoint;
        this.http403AccessDeniedHandler = http403AccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (not needed for REST APIs)
            .csrf(csrf -> csrf.disable())

            // JWT filter before username/password auth
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow access to Swagger documentation
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/api-docs/**"
                ).permitAll()

                // Allow access to Actuator health checks
                .requestMatchers("/actuator/**").permitAll()

                // Auth: profile and change-password require authenticated user
                .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/auth/change-password").authenticated()
                // Auth: login etc. - no token required
                .requestMatchers("/auth/**").permitAll()

                // Restaurant: my-restaurant (restaurant admin only; waiter/kitchen cannot edit)
                .requestMatchers(HttpMethod.GET, "/restaurants/me").hasRole("RESTAURANT_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/restaurants/me").hasRole("RESTAURANT_ADMIN")
                // Restaurant: only platform admin can create/update/delete/list
                .requestMatchers(HttpMethod.POST, "/restaurants").hasRole("PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/restaurants/*").hasRole("PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/restaurants/*").hasRole("PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/restaurants", "/restaurants/*").hasRole("PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/restaurants/*/users").authenticated()
                .requestMatchers(HttpMethod.POST, "/restaurants/*/users").authenticated()
                .requestMatchers(HttpMethod.PUT, "/restaurants/*/users/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/restaurants/*/users/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/restaurants/*/users/*/reset-password").authenticated()

                // Platform admin management: only platform admin can list and create
                .requestMatchers(HttpMethod.GET, "/platform-admins").hasRole("PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/platform-admins").hasRole("PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/platform-admins/*").hasRole("PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/platform-admins/*").hasRole("PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/platform-admins/*/reset-password").hasRole("PLATFORM_ADMIN")

                // All other requests require authenticated user
                .anyRequest().authenticated()
            )

            // Stateless session (REST API doesn't need sessions)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 401 for unauthenticated, 403 for authenticated but not authorized
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(http401EntryPoint)
                    .accessDeniedHandler(http403AccessDeniedHandler));

        return http.build();
    }

    /**
     * Password encoder using BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expose AuthenticationManager for programmatic login (e.g. AuthController).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
