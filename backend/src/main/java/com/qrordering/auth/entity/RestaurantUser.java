package com.qrordering.auth.entity;

import com.qrordering.auth.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Restaurant User Entity
 *
 * Represents a user (employee) within a specific restaurant (tenant).
 * Each restaurant can have multiple users with different roles.
 */
@Entity
@Table(name = "restaurant_user",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "username"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantUser {

    /**
     * User ID (auto-generated)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Tenant ID (Restaurant ID)
     * Links this user to a specific restaurant
     */
    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    /**
     * Username (unique within the same tenant)
     */
    @Column(name = "username", length = 50, nullable = false)
    private String username;

    /**
     * Password (BCrypt encrypted)
     */
    @Column(name = "password", length = 255, nullable = false)
    private String password;

    /**
     * User role within the restaurant
     * Stored as Integer in database (1=RESTAURANT_ADMIN, 2=WAITER, 3=KITCHEN)
     * Automatically converted by UserRoleConverter
     */
    @Convert(converter = com.qrordering.auth.converter.UserRoleConverter.class)
    @Column(name = "role", nullable = false)
    private UserRole role;

    /**
     * User email
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * Creation timestamp
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Equals based on entity ID
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RestaurantUser)) return false;
        RestaurantUser that = (RestaurantUser) o;
        return Objects.equals(id, that.id);
    }

    /**
     * HashCode based on entity ID
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
