package com.kinyozi.royale.repository;
import com.kinyozi.royale.model.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {
  List<ServiceItem> findByTenantId(UUID tenantId);
}
