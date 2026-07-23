package com.kinyozi.royale.controller;

import com.kinyozi.royale.exception.NotFoundException;
import com.kinyozi.royale.model.Tenant;
import com.kinyozi.royale.repository.TenantRepository;
import com.kinyozi.royale.service.TenantContext;

import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Per-tenant configuration endpoints. Only the OWNER role may mutate the
 * settings; all authenticated members can read them so the POS UI knows
 * whether to require a worker at checkout.
 */
@RestController
@RequestMapping("/tenant/settings")
public class TenantSettingsController {

    private final TenantRepository tenants;

    public TenantSettingsController(TenantRepository tenants) { this.tenants = tenants; }

    public record TenantSettingsResponse(Tenant.WorkerAssignmentMode workerAssignmentMode) {}
    public record UpdateTenantSettingsRequest(@NotNull Tenant.WorkerAssignmentMode workerAssignmentMode) {}

    @GetMapping
    public TenantSettingsResponse get() {
        Tenant t = tenants.findById(TenantContext.current())
                .orElseThrow(() -> new NotFoundException("Tenant not found"));
        Tenant.WorkerAssignmentMode mode = t.getWorkerAssignmentMode() == null
                ? Tenant.WorkerAssignmentMode.BEFORE_CHECKOUT
                : t.getWorkerAssignmentMode();
        return new TenantSettingsResponse(mode);
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public TenantSettingsResponse update(@org.springframework.web.bind.annotation.RequestBody
                                          @jakarta.validation.Valid UpdateTenantSettingsRequest body) {
        Tenant t = tenants.findById(TenantContext.current())
                .orElseThrow(() -> new NotFoundException("Tenant not found"));
        t.setWorkerAssignmentMode(body.workerAssignmentMode());
        tenants.save(t);
        return new TenantSettingsResponse(t.getWorkerAssignmentMode());
    }
}
