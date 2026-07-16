package com.kinyozi.royale.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_payment")
public class CreditPayment {
    @Id @GeneratedValue private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "credit_id", nullable = false) private UUID creditId;
    @Column(nullable = false) private BigDecimal amount;
    @Column(name = "paid_at") private LocalDateTime paidAt = LocalDateTime.now();
    private String note;
    @Column(name = "created_by") private String createdBy;

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getTenantId() { return tenantId; } public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getCreditId() { return creditId; } public void setCreditId(UUID v) { this.creditId = v; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { this.amount = v; }
    public LocalDateTime getPaidAt() { return paidAt; } public void setPaidAt(LocalDateTime v) { this.paidAt = v; }
    public String getNote() { return note; } public void setNote(String v) { this.note = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { this.createdBy = v; }
}
