package com.kinyozi.royale.dto;

import java.math.BigDecimal;

public class InventoryUpdateRequest {
    private String name;
    private String category;
    private String unit;
    private BigDecimal currentQty;
    private BigDecimal reorderLevel;
    private BigDecimal pricePerUnit;
    private String description;

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { this.unit = v; }
    public BigDecimal getCurrentQty() { return currentQty; }
    public void setCurrentQty(BigDecimal v) { this.currentQty = v; }
    public BigDecimal getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(BigDecimal v) { this.reorderLevel = v; }
    public BigDecimal getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(BigDecimal v) { this.pricePerUnit = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
}
