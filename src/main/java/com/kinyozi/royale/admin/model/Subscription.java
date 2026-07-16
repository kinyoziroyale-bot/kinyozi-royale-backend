package com.kinyozi.royale.admin.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A renewal / payment record for a tenant's subscription. The current
 * expiry_date on tenant_admin_meta is kept in sync with the latest row
 * here so status computation stays fast and consistent.
 *
 * v5: extended with payment method, M-Pesa reference, previous expiry
 * snapshot, business code snapshot and the admin who performed the
 * renewal so the history dialog can show a full audit trail.
 */
@Entity
@Table(name = "subscriptions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Subscription {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Snapshot of the tenant's business_code at the time of renewal. */
    @Column(name = "business_code", length = 64)
    private String businessCode;

    @Column(nullable = false, length = 64)
    private String plan;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    /** Expiry that was in effect before this renewal was applied. */
    @Column(name = "previous_expiry_date")
    private LocalDate previousExpiryDate;

    @Column(name = "amount_paid", precision = 14, scale = 2)
    private BigDecimal amountPaid;

    /** e.g. MPESA, CASH, BANK, CARD, OTHER. */
    @Column(name = "payment_method", length = 32)
    private String paymentMethod;

    /**
     * Kept for backwards compatibility with earlier v4 history rows and
     * the current renewal payload. New renewals populate {@link #mpesaReference}
     * as well when the method is M-Pesa.
     */
    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    /** M-Pesa transaction ID / confirmation message. */
    @Column(name = "mpesa_reference", length = 255)
    private String mpesaReference;

    @Column(name = "payment_notes", length = 1000)
    private String paymentNotes;

    /** Username of the platform admin who performed the renewal. */
    @Column(name = "performed_by", length = 128)
    private String performedBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}
