package com.kinyozi.royale.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "worker_commission")
public class WorkerCommission {
    @Id @GeneratedValue private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "worker_id", nullable = false) private UUID workerId;
    @Column(name = "service_id") private UUID serviceId; // null = applies to all services
    private BigDecimal percent;
    @Column(name = "fixed_amount") private BigDecimal fixedAmount;
    private boolean active = true;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at") private LocalDateTime updatedAt = LocalDateTime.now();

    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; } public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getWorkerId() { return workerId; } public void setWorkerId(UUID v) { this.workerId = v; }
    public UUID getServiceId() { return serviceId; } public void setServiceId(UUID v) { this.serviceId = v; }
    public BigDecimal getPercent() { return percent; } public void setPercent(BigDecimal v) { this.percent = v; }
    public BigDecimal getFixedAmount() { return fixedAmount; } public void setFixedAmount(BigDecimal v) { this.fixedAmount = v; }
    public boolean isActive() { return active; } public void setActive(boolean v) { this.active = v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
