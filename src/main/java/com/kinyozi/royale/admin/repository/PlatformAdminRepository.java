package com.kinyozi.royale.admin.repository;

import com.kinyozi.royale.admin.model.PlatformAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, UUID> {
    Optional<PlatformAdmin> findByUsername(String username);
    boolean existsByUsername(String username);
}
