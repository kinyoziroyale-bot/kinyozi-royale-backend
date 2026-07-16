package com.kinyozi.royale.admin.controller;

import com.kinyozi.royale.admin.dto.AdminDtos.*;
import com.kinyozi.royale.admin.service.SubscriptionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/subscriptions")
public class AdminSubscriptionsController {

    private final SubscriptionService svc;
    public AdminSubscriptionsController(SubscriptionService svc) { this.svc = svc; }

    @GetMapping
    public List<SubscriptionRow> list(@RequestParam(name = "q", required = false) String q,
                                      @RequestParam(name = "status", required = false) String status) {
        return svc.list(q, status);
    }

    @GetMapping("/summary")
    public SubscriptionSummary summary() { return svc.summary(); }

    @GetMapping("/{tenantId}/history")
    public List<SubscriptionHistoryRow> history(@PathVariable UUID tenantId) {
        return svc.history(tenantId);
    }

    @PostMapping("/{tenantId}/renew")
    public SubscriptionRow renew(@PathVariable UUID tenantId,
                                 @RequestBody RenewSubscriptionRequest body) {
        return svc.renew(tenantId, body);
    }
}
