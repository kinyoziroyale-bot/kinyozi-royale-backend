package com.kinyozi.royale.repository;

import com.kinyozi.royale.model.Credit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface CreditRepository extends JpaRepository<Credit, UUID> {
    List<Credit> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<Credit> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(UUID tenantId, UUID customerId);

    @Query("select c from Credit c where c.tenantId = :t and " +
           "(lower(coalesce(c.note,'')) like :q or cast(c.customerId as string) like :q)")
    List<Credit> search(@Param("t") UUID tenantId, @Param("q") String q);
}
