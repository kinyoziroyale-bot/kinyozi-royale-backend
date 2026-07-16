package com.kinyozi.royale.admin.service;

import com.kinyozi.royale.admin.dto.AdminDtos.*;
import com.kinyozi.royale.admin.model.Subscription;
import com.kinyozi.royale.admin.model.TenantAdminMeta;
import com.kinyozi.royale.admin.model.TenantAdminMeta.Status;
import com.kinyozi.royale.admin.repository.SubscriptionRepository;
import com.kinyozi.royale.admin.repository.TenantAdminMetaRepository;
import com.kinyozi.royale.admin.security.AdminPrincipal;
import com.kinyozi.royale.exception.BadRequestException;
import com.kinyozi.royale.exception.NotFoundException;
import com.kinyozi.royale.model.Tenant;
import com.kinyozi.royale.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * Subscription listing + renewal service used by the developer portal
 * Subscriptions page.
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);


    private final TenantRepository tenants;
    private final TenantAdminMetaRepository metas;
    private final SubscriptionRepository subs;

    public SubscriptionService(TenantRepository tenants,
                               TenantAdminMetaRepository metas,
                               SubscriptionRepository subs) {
        this.tenants = tenants;
        this.metas = metas;
        this.subs = subs;
    }

    public static Status computeStatus(TenantAdminMeta m) {
        // Fail-secure: a tenant without a subscription metadata row must
        // never be treated as an implicit ACTIVE license. Registration now
        // always creates a metadata row, and SchemaFixRunner back-fills any
        // legacy tenants at startup, so reaching this branch in production
        // means something is wrong — deny by returning EXPIRED so the
        // Subscription Expired gate takes over instead of granting access.
        if (m == null) return Status.EXPIRED;
        if (m.isDeleted() || m.getStatus() == Status.DELETED) return Status.DELETED;
        if (m.getStatus() == Status.SUSPENDED) return Status.SUSPENDED;
        LocalDate today = LocalDate.now();
        if (m.getExpiryDate() != null && today.isAfter(m.getExpiryDate())) return Status.EXPIRED;
        if (m.getStatus() == Status.TRIAL) return Status.TRIAL;
        return Status.ACTIVE;
    }

    private TenantAdminMeta metaFor(UUID tenantId) {
        return metas.findById(tenantId).orElseGet(() ->
                metas.save(TenantAdminMeta.builder().tenantId(tenantId).build()));
    }

    /* ---------------- listing ---------------- */

    public List<SubscriptionRow> list(String query, String statusFilter) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Tenant> all = tenants.findAll();
        List<SubscriptionRow> out = new ArrayList<>();
        for (Tenant t : all) {
            TenantAdminMeta m = metas.findById(t.getId()).orElse(null);
            Status st = computeStatus(m);
            if (st == Status.DELETED) continue;
            if (statusFilter != null && !statusFilter.isBlank()
                    && !statusFilter.equalsIgnoreCase(st.name())) continue;

            Subscription latest = subs.findFirstByTenantIdOrderByCreatedAtDesc(t.getId()).orElse(null);
            String plan = m != null && m.getSubscriptionPlan() != null
                    ? m.getSubscriptionPlan()
                    : (latest != null ? latest.getPlan() : null);

            if (!q.isEmpty()) {
                String hay = ((t.getBusinessName() == null ? "" : t.getBusinessName()) + " " +
                        (t.getBusinessCode() == null ? "" : t.getBusinessCode()) + " " +
                        (t.getOwnerName() == null ? "" : t.getOwnerName()) + " " +
                        (t.getPhone() == null ? "" : t.getPhone()) + " " +
                        (plan == null ? "" : plan)).toLowerCase(Locale.ROOT);
                if (!hay.contains(q)) continue;
            }

            out.add(new SubscriptionRow(
                    t.getId(), t.getBusinessName(), t.getBusinessCode(),
                    plan,
                    latest == null ? null : latest.getStartDate(),
                    m == null ? null : m.getExpiryDate(),
                    st.name(),
                    latest == null ? null : latest.getAmountPaid(),
                    latest == null ? null : (latest.getMpesaReference() != null
                            ? latest.getMpesaReference() : latest.getPaymentReference()),
                    latest == null ? null : latest.getCreatedAt(),
                    m == null ? null : m.getUpdatedAt()));
        }
        out.sort(Comparator.comparing(
                (SubscriptionRow r) -> r.expiryDate(),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    public SubscriptionSummary summary() {
        long active = 0, trial = 0, suspended = 0, expired = 0;
        for (Tenant t : tenants.findAll()) {
            TenantAdminMeta m = metas.findById(t.getId()).orElse(null);
            Status st = computeStatus(m);
            switch (st) {
                case ACTIVE    -> active++;
                case TRIAL     -> trial++;
                case SUSPENDED -> suspended++;
                case EXPIRED   -> expired++;
                default -> {}
            }
        }
        return new SubscriptionSummary(active, trial, suspended, expired);
    }

    public List<SubscriptionHistoryRow> history(UUID tenantId) {
        Tenant t = tenants.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Business not found"));
        return subs.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(s -> new SubscriptionHistoryRow(
                        s.getId(),
                        tenantId,
                        t.getBusinessName(),
                        s.getBusinessCode() != null ? s.getBusinessCode() : t.getBusinessCode(),
                        s.getPlan(),
                        s.getStartDate(),
                        s.getExpiryDate(),
                        s.getPreviousExpiryDate(),
                        s.getAmountPaid(),
                        s.getPaymentMethod(),
                        s.getMpesaReference(),
                        s.getPaymentReference(),
                        s.getPaymentNotes(),
                        s.getPerformedBy(),
                        s.getCreatedAt()))
                .toList();
    }

    /* ---------------- renewal ---------------- */

    @Transactional
    public SubscriptionRow renew(UUID tenantId, RenewSubscriptionRequest req) {
        Tenant t = tenants.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Business not found"));
        if (req == null) throw new BadRequestException("Renewal payload required");

        String plan = req.plan() == null || req.plan().isBlank() ? "STANDARD" : req.plan().trim();

        LocalDate today = LocalDate.now();
        LocalDate start = today;

        TenantAdminMeta existing = metas.findById(tenantId).orElse(null);
        LocalDate previousExpiry = existing == null ? null : existing.getExpiryDate();

        LocalDate expiry;
        if (req.customExpiryDate() != null) {
            expiry = req.customExpiryDate();
        } else {
            int days = req.durationDays() == null ? 30 : Math.max(1, req.durationDays());
            LocalDate base = today;
            if (previousExpiry != null && previousExpiry.isAfter(today)) {
                base = previousExpiry;
            }
            expiry = base.plusDays(days);
        }
        if (expiry.isBefore(today)) {
            throw new BadRequestException("Expiry date must be in the future");
        }

        String method = req.paymentMethod() == null || req.paymentMethod().isBlank()
                ? null : req.paymentMethod().trim().toUpperCase(Locale.ROOT);
        String mpesa = req.mpesaReference() == null || req.mpesaReference().isBlank()
                ? null : req.mpesaReference().trim();
        String ref = req.paymentReference() == null || req.paymentReference().isBlank()
                ? mpesa : req.paymentReference().trim();

        Subscription s = Subscription.builder()
                .tenantId(tenantId)
                .businessCode(t.getBusinessCode())
                .plan(plan)
                .startDate(start)
                .expiryDate(expiry)
                .previousExpiryDate(previousExpiry)
                .amountPaid(req.amountPaid())
                .paymentMethod(method)
                .mpesaReference(mpesa)
                .paymentReference(ref)
                .paymentNotes(req.paymentNotes())
                .performedBy(AdminPrincipal.username())
                .build();
        s = subs.save(s);

        TenantAdminMeta m = metaFor(tenantId);
        m.setSubscriptionPlan(plan);
        m.setExpiryDate(expiry);
        if (m.getStatus() == Status.EXPIRED || m.getStatus() == null) {
            m.setStatus(Status.ACTIVE);
        } else if (m.getStatus() == Status.SUSPENDED && expiry.isAfter(today)) {
            m.setStatus(Status.ACTIVE);
            m.setSuspendedAt(null);
            m.setSuspendedReason(null);
        }
        metas.save(m);
        log.info("AUDIT admin-subscription-renew: actor={} tenant={} plan={} expiry={} amount={}",
                AdminPrincipal.username(), tenantId, plan, expiry, s.getAmountPaid());

        return new SubscriptionRow(
                t.getId(), t.getBusinessName(), t.getBusinessCode(),
                plan, s.getStartDate(), expiry, computeStatus(m).name(),
                s.getAmountPaid(),
                s.getMpesaReference() != null ? s.getMpesaReference() : s.getPaymentReference(),
                s.getCreatedAt(), m.getUpdatedAt());
    }
}
