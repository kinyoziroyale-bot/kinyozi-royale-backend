package com.kinyozi.royale.admin.repository;

import com.kinyozi.royale.admin.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<Subscription> findFirstByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
