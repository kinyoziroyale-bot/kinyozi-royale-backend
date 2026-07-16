package com.kinyozi.royale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Whitelisted create payload for InventoryItem. Server-owned fields
 * (id, tenantId, createdAt) are not accepted.
 */
public class InventoryItemCreateRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 120)
    private String category;

    @Size(max = 40)
    private String unit;

    @PositiveOrZero
    private Integer currentQty;

    @PositiveOrZero
    private Integer reorderLevel;

    @PositiveOrZero
    private Integer pricePerUnit;

    @Size(max = 2000)
    private String description;

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { this.unit = v; }
    public Integer getCurrentQty() { return currentQty; }
    public void setCurrentQty(Integer v) { this.currentQty = v; }
    public Integer getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Integer v) { this.reorderLevel = v; }
    public Integer getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(Integer v) { this.pricePerUnit = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
}
