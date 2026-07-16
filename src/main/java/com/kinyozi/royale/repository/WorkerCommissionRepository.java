package com.kinyozi.royale.repository;

import com.kinyozi.royale.model.WorkerCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkerCommissionRepository extends JpaRepository<WorkerCommission, UUID> {
    List<WorkerCommission> findByTenantId(UUID tenantId);
    List<WorkerCommission> findByTenantIdAndWorkerId(UUID tenantId, UUID workerId);
    Optional<WorkerCommission> findByTenantIdAndWorkerIdAndServiceId(UUID tenantId, UUID workerId, UUID serviceId);
}
