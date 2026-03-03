package com.qrordering.restaurant.entity;

import com.qrordering.restaurant.enums.RestaurantStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Restaurant Entity
 *
 * Represents a restaurant (tenant) in the multi-tenant SaaS platform.
 * Each restaurant is an independent tenant with isolated data.
 */
@Entity
@Table(name = "restaurant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

    /**
     * Restaurant ID (tenant ID)
     * Automatically generated in format: REST_YYYYMMDD_XXXXXXXXXXXX
     * where XXXXXXXXXXXX is 12 hex characters from UUID
     */
    @Id
    @Column(name = "id", length = 50)
    private String id;

    /**
     * Restaurant name
     */
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /**
     * Restaurant description
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Logo URL
     */
    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    /**
     * Restaurant address
     */
    @Column(name = "address", length = 255)
    private String address;

    /**
     * Contact phone number
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Restaurant status
     * Stored as Integer in database (1=ACTIVE, 2=SUSPENDED, 3=INACTIVE)
     * Automatically converted by RestaurantStatusConverter
     */
    @Convert(converter = com.qrordering.restaurant.converter.RestaurantStatusConverter.class)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RestaurantStatus status = RestaurantStatus.ACTIVE;

    /**
     * Creation timestamp
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Equals based on entity ID
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Restaurant)) return false;
        Restaurant that = (Restaurant) o;
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
