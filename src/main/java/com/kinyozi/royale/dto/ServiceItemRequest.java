package com.kinyozi.royale.dto;

import com.kinyozi.royale.model.CommissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Whitelisted create/update payload for ServiceItem endpoints. Server-owned
 * fields (id, tenantId, updatedAt) are not accepted.
 */
public class ServiceItemRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 120)
    private String category;

    @PositiveOrZero
    private Integer durationMin;

    @PositiveOrZero
    private Integer price;

    private Boolean active;

    private CommissionType commissionType;

    @PositiveOrZero
    private BigDecimal commissionValue;

    private Set<UUID> categoryIds = new HashSet<>();

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer v) { this.durationMin = v; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer v) { this.price = v; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean v) { this.active = v; }
    public CommissionType getCommissionType() { return commissionType; }
    public void setCommissionType(CommissionType v) { this.commissionType = v; }
    public BigDecimal getCommissionValue() { return commissionValue; }
    public void setCommissionValue(BigDecimal v) { this.commissionValue = v; }
    public Set<UUID> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(Set<UUID> v) { this.categoryIds = (v == null ? new HashSet<>() : v); }
}
