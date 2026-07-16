package com.kinyozi.royale.repository;

import com.kinyozi.royale.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    boolean existsByBusinessCode(String businessCode);
}
