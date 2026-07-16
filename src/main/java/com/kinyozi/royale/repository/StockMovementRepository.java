package com.kinyozi.royale.repository;
import com.kinyozi.royale.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
  List<StockMovement> findByTenantId(UUID tenantId);

    List<StockMovement> findByTenantIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            UUID tenantId, Instant cutoff);

}
