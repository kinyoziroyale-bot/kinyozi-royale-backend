package com.kinyozi.royale.controller;

import com.kinyozi.royale.dto.InventoryItemCreateRequest;
import com.kinyozi.royale.dto.InventoryUpdateRequest;
import com.kinyozi.royale.exception.NotFoundException;
import com.kinyozi.royale.model.InventoryItem;
import com.kinyozi.royale.repository.InventoryItemRepository;
import com.kinyozi.royale.security.CurrentUser;
import org.springframework.security.core.Authentication;
import com.kinyozi.royale.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Inventory endpoints. Uses {@link InventoryItemCreateRequest} for create
 * so server-owned fields (id, tenantId, createdAt) cannot be mass-assigned.
 * Update path already uses {@link InventoryUpdateRequest}.
 */
@RestController
@RequestMapping("/inventory")
public class InventoryItemController {
    private final InventoryItemRepository repo;
    private final InventoryService inventoryService;

    public InventoryItemController(InventoryItemRepository r, InventoryService inventoryService) {
        this.repo = r;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryItem> list() { return repo.findByTenantId(CurrentUser.tenantId()); }

    @GetMapping("/{id}")
    public InventoryItem get(@PathVariable UUID id) {
        InventoryItem e = repo.findById(id).orElseThrow(() -> new NotFoundException("Not found"));
        if (!e.getTenantId().equals(CurrentUser.tenantId())) throw new NotFoundException("Not found");
        return e;
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public InventoryItem create(@Valid @RequestBody InventoryItemCreateRequest body) {
        InventoryItem i = InventoryItem.builder()
                .tenantId(CurrentUser.tenantId())
                .name(body.getName())
                .category(body.getCategory())
                .unit(body.getUnit())
                .currentQty(body.getCurrentQty() == null ? 0 : body.getCurrentQty())
                .reorderLevel(body.getReorderLevel() == null ? 0 : body.getReorderLevel())
                .pricePerUnit(body.getPricePerUnit() == null ? 0 : body.getPricePerUnit())
                .description(body.getDescription())
                .build();
        return repo.save(i);
    }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public InventoryItem update(@PathVariable UUID id, @RequestBody InventoryUpdateRequest body) {
        return inventoryService.update(id, body);
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        inventoryService.delete(id);
    }
}
