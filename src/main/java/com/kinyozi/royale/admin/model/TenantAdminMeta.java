package com.kinyozi.royale.admin.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Developer-portal-owned metadata for a tenant.
 *
 * V2: adds TRIAL / EXPIRED statuses and subscription/lastLogin fields.
 * The columns are all nullable so existing rows keep working.
 */
@Entity
@Table(name = "tenant_admin_meta")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TenantAdminMeta {

    public enum Status { ACTIVE, TRIAL, EXPIRED, SUSPENDED, DELETED }

    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspended_reason", length = 500)
    private String suspendedReason;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "subscription_plan", length = 50)
    private String subscriptionPlan;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}
