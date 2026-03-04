package com.qrordering.auth.repository;

import com.qrordering.auth.entity.PlatformAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for platform administrators.
 */
public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, Long> {

    Optional<PlatformAdmin> findByUsername(String username);
}
