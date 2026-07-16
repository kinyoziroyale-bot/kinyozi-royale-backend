package com.kinyozi.royale.admin.repository;

import com.kinyozi.royale.admin.model.TenantAdminMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantAdminMetaRepository extends JpaRepository<TenantAdminMeta, UUID> {
    List<TenantAdminMeta> findByStatus(TenantAdminMeta.Status status);
    long countByStatus(TenantAdminMeta.Status status);
}
