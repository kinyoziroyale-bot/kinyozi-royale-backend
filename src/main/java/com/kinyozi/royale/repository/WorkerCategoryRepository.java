package com.kinyozi.royale.repository;

import com.kinyozi.royale.model.WorkerCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkerCategoryRepository extends JpaRepository<WorkerCategory, UUID> {
    List<WorkerCategory> findByTenantId(UUID tenantId);
    Optional<WorkerCategory> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
