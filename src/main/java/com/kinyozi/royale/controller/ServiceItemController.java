package com.kinyozi.royale.controller;

import com.kinyozi.royale.dto.ServiceItemRequest;
import com.kinyozi.royale.exception.NotFoundException;
import com.kinyozi.royale.model.ServiceItem;
import com.kinyozi.royale.repository.ServiceItemRepository;
import com.kinyozi.royale.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Service catalog endpoints. Uses {@link ServiceItemRequest} to prevent
 * mass-assignment of server-owned fields.
 */
@RestController
@RequestMapping("/services")
public class ServiceItemController {
    private final ServiceItemRepository repo;

    public ServiceItemController(ServiceItemRepository r) { this.repo = r; }

    @GetMapping
    public List<ServiceItem> list() { return repo.findByTenantId(CurrentUser.tenantId()); }

    @GetMapping("/{id}")
    public ServiceItem get(@PathVariable UUID id) {
        ServiceItem e = repo.findById(id).orElseThrow(() -> new NotFoundException("Not found"));
        if (!e.getTenantId().equals(CurrentUser.tenantId())) throw new NotFoundException("Not found");
        return e;
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public ServiceItem create(@Valid @RequestBody ServiceItemRequest body) {
        ServiceItem s = ServiceItem.builder()
                .tenantId(CurrentUser.tenantId())
                .name(body.getName())
                .category(body.getCategory())
                .durationMin(body.getDurationMin() == null ? 30 : body.getDurationMin())
                .price(body.getPrice() == null ? 0 : body.getPrice())
                .active(body.getActive() == null ? true : body.getActive())
                .commissionType(body.getCommissionType())
                .commissionValue(body.getCommissionValue())
                .categoryIds(body.getCategoryIds() == null ? new HashSet<>() : body.getCategoryIds())
                .updatedAt(Instant.now())
                .build();
        return repo.save(s);
    }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public ServiceItem update(@PathVariable UUID id, @Valid @RequestBody ServiceItemRequest body) {
        ServiceItem e = repo.findById(id).orElseThrow(() -> new NotFoundException("Not found"));
        if (!e.getTenantId().equals(CurrentUser.tenantId())) throw new NotFoundException("Not found");
        // Mutate only whitelisted fields — id, tenantId stay server-owned.
        e.setName(body.getName());
        e.setCategory(body.getCategory());
        if (body.getDurationMin() != null) e.setDurationMin(body.getDurationMin());
        if (body.getPrice() != null) e.setPrice(body.getPrice());
        if (body.getActive() != null) e.setActive(body.getActive());
        e.setCommissionType(body.getCommissionType());
        e.setCommissionValue(body.getCommissionValue());
        if (body.getCategoryIds() != null) e.setCategoryIds(body.getCategoryIds());
        e.setUpdatedAt(Instant.now());
        return repo.save(e);
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        ServiceItem e = repo.findById(id).orElseThrow(() -> new NotFoundException("Not found"));
        if (!e.getTenantId().equals(CurrentUser.tenantId())) throw new NotFoundException("Not found");
        repo.deleteById(id);
    }
}
