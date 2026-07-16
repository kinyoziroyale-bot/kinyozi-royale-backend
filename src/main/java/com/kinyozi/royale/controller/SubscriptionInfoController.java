package com.kinyozi.royale.controller;

import com.kinyozi.royale.admin.model.TenantAdminMeta;
import com.kinyozi.royale.admin.repository.TenantAdminMetaRepository;
import com.kinyozi.royale.admin.service.SubscriptionService;
import com.kinyozi.royale.exception.NotFoundException;
import com.kinyozi.royale.model.Tenant;
import com.kinyozi.royale.repository.TenantRepository;
import com.kinyozi.royale.security.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Tenant-facing subscription info. This endpoint is intentionally
 * reachable even when the subscription has expired so the Client Portal
 * can display the Subscription Expired page with accurate data.
 */
@RestController
@RequestMapping("/subscription")
public class SubscriptionInfoController {

    private final TenantRepository tenants;
    private final TenantAdminMetaRepository metas;

    public SubscriptionInfoController(TenantRepository tenants, TenantAdminMetaRepository metas) {
        this.tenants = tenants;
        this.metas = metas;
    }

    public record MySubscriptionResponse(
            String tenantId,
            String businessName,
            String businessCode,
            String plan,
            LocalDate expiryDate,
            String status,
            boolean expired
    ) {}

    @GetMapping("/me")
    public MySubscriptionResponse me() {
        UUID tenantId = CurrentUser.tenantId();
        Tenant t = tenants.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found"));
        TenantAdminMeta m = metas.findById(tenantId).orElse(null);
        TenantAdminMeta.Status status = SubscriptionService.computeStatus(m);
        LocalDate expiry = m == null ? null : m.getExpiryDate();
        boolean expired = expiry != null && LocalDate.now().isAfter(expiry);
        return new MySubscriptionResponse(
                t.getId().toString(),
                t.getBusinessName(),
                t.getBusinessCode(),
                m == null ? null : m.getSubscriptionPlan(),
                expiry,
                status.name(),
                expired
        );
    }
}
