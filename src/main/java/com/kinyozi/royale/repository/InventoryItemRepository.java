package com.kinyozi.royale.repository;
import com.kinyozi.royale.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
  List<InventoryItem> findByTenantId(UUID tenantId);
}
