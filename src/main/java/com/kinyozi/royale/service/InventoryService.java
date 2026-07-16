package com.kinyozi.royale.service;

import com.kinyozi.royale.dto.InventoryUpdateRequest;
import com.kinyozi.royale.exception.NotFoundException;
import com.kinyozi.royale.model.InventoryItem;
import com.kinyozi.royale.repository.InventoryItemRepository;
import com.kinyozi.royale.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryRepo;

    public InventoryService(InventoryItemRepository inventoryRepo) {
        this.inventoryRepo = inventoryRepo;
    }

    @Transactional
    public InventoryItem update(UUID id, InventoryUpdateRequest req) {
        InventoryItem item = loadTenantItem(id);

        if (req.getName() != null) item.setName(req.getName());
        if (req.getCategory() != null) item.setCategory(req.getCategory());
        if (req.getUnit() != null) item.setUnit(req.getUnit());
        if (req.getCurrentQty() != null) item.setCurrentQty(req.getCurrentQty().intValue());
        if (req.getReorderLevel() != null) item.setReorderLevel(req.getReorderLevel().intValue());
        if (req.getPricePerUnit() != null) item.setPricePerUnit(req.getPricePerUnit().intValue());
        if (req.getDescription() != null) item.setDescription(req.getDescription());

        return inventoryRepo.save(item);
    }

    @Transactional
    public void delete(UUID id) {
        InventoryItem item = loadTenantItem(id);
        inventoryRepo.delete(item);
    }

    private InventoryItem loadTenantItem(UUID id) {
        InventoryItem item = inventoryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        if (!item.getTenantId().equals(CurrentUser.tenantId())) {
            throw new NotFoundException("Item not found");
        }
        return item;
    }
}
