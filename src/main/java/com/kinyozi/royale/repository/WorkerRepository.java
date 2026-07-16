package com.kinyozi.royale.repository;

import com.kinyozi.royale.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {
    List<Worker> findByTenantId(UUID tenantId);
    List<Worker> findByTenantIdAndActive(UUID tenantId, boolean active);
}
