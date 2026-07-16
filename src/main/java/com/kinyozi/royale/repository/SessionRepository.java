package com.kinyozi.royale.repository;

import com.kinyozi.royale.model.CustomerSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<CustomerSession, UUID> {
    List<CustomerSession> findByTenantId(UUID tenantId);
    List<CustomerSession> findByTenantIdAndStatus(UUID tenantId, CustomerSession.Status status);
    List<CustomerSession> findByTenantIdAndStatusAndClosedAtBetween(
            UUID tenantId, CustomerSession.Status status, Instant from, Instant to);
}
