package com.kinyozi.royale.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit")
public class Credit {
    @Id @GeneratedValue private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "session_id") private UUID sessionId;

    // Legacy NOT NULL column in DB — kept in sync with totalOwed
    @Column(name = "amount", nullable = false) private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "total_owed", nullable = false) private BigDecimal totalOwed;
    @Column(name = "total_paid", nullable = false) private BigDecimal totalPaid = BigDecimal.ZERO;
    @Column(nullable = false) private String status = "OPEN";
    private String note;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at") private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist @PreUpdate
    void syncAmount() {
        if (totalOwed == null) totalOwed = BigDecimal.ZERO;
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        this.amount = totalOwed; // keep legacy column populated
    }

    public BigDecimal balance() {
        BigDecimal paid = totalPaid == null ? BigDecimal.ZERO : totalPaid;
        return totalOwed.subtract(paid);
    }

    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; } public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getCustomerId() { return customerId; } public void setCustomerId(UUID v) { this.customerId = v; }
    public UUID getSessionId() { return sessionId; } public void setSessionId(UUID v) { this.sessionId = v; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { this.amount = v; }
    public BigDecimal getTotalOwed() { return totalOwed; } public void setTotalOwed(BigDecimal v) { this.totalOwed = v; }
    public BigDecimal getTotalPaid() { return totalPaid; } public void setTotalPaid(BigDecimal v) { this.totalPaid = v; }
    public String getStatus() { return status; } public void setStatus(String v) { this.status = v; }
    public String getNote() { return note; } public void setNote(String v) { this.note = v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
