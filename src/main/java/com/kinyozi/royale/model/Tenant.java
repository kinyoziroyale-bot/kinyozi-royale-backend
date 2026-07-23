package com.kinyozi.royale.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"business_code"}) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "business_name", nullable = false, length = 255)
    private String businessName;

    @Column(name = "owner_name", length = 255)
    private String ownerName;

    @Column(length = 50)
    private String phone;

    /**
     * Permanent, human-friendly business code (e.g. KR-000123). Generated
     * exactly once at registration and never changes. Unique per platform.
     */
    @Column(name = "business_code", length = 32, unique = true)
    private String businessCode;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** How this tenant assigns workers to POS line items. */
    public enum WorkerAssignmentMode { BEFORE_CHECKOUT, AFTER_SERVICE }

    @Enumerated(EnumType.STRING)
    @Column(name = "worker_assignment_mode", nullable = false, length = 32)
    @Builder.Default
    private WorkerAssignmentMode workerAssignmentMode = WorkerAssignmentMode.BEFORE_CHECKOUT;
}
