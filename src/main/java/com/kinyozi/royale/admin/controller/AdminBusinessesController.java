package com.kinyozi.royale.admin.controller;

import com.kinyozi.royale.admin.dto.AdminDtos.*;
import com.kinyozi.royale.admin.service.AdminBusinessService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Developer Portal business management endpoints.
 *
 * Modern REST verbs are PATCH for state transitions and DELETE for soft
 * removal. The legacy POST activate/suspend endpoints are preserved for
 * backwards compatibility with any older client build.
 */
@RestController
@RequestMapping("/admin/businesses")
public class AdminBusinessesController {

    private final AdminBusinessService svc;

    public AdminBusinessesController(AdminBusinessService s) { this.svc = s; }

    @GetMapping
    public BusinessPage list(@RequestParam(name = "q", required = false) String q,
                             @RequestParam(name = "status", required = false) String status,
                             @RequestParam(name = "page", defaultValue = "0") int page,
                             @RequestParam(name = "size", defaultValue = "25") int size,
                             @RequestParam(name = "showDeleted", defaultValue = "false") boolean showDeleted) {
        return svc.list(q, status, page, Math.max(1, Math.min(size, 200)), showDeleted);
    }

    @GetMapping("/{id}")
    public BusinessDetail get(@PathVariable UUID id) { return svc.detail(id); }

    @GetMapping("/{id}/stats")
    public BusinessStats stats(@PathVariable UUID id) { return svc.statsFor(id); }

    @GetMapping("/{id}/workers")     public List<WorkerDto> workers(@PathVariable UUID id) { return svc.workers(id); }
    @GetMapping("/{id}/customers")   public List<CustomerDto> customers(@PathVariable UUID id) { return svc.customers(id); }
    @GetMapping("/{id}/services")    public List<ServiceDto> services(@PathVariable UUID id) { return svc.services(id); }
    @GetMapping("/{id}/inventory")   public List<InventoryDto> inventory(@PathVariable UUID id) { return svc.inventory(id); }
    @GetMapping("/{id}/credits")     public List<CreditDto> credits(@PathVariable UUID id) { return svc.credits(id); }
    @GetMapping("/{id}/worker-earnings") public List<WorkerEarningDto> earnings(@PathVariable UUID id) { return svc.workerEarnings(id); }

    /* ----- state transitions ----- */

    @PatchMapping("/{id}/activate")
    public StatusResponse activatePatch(@PathVariable UUID id) { return svc.activate(id); }

    @PatchMapping("/{id}/suspend")
    public StatusResponse suspendPatch(@PathVariable UUID id,
                                       @RequestBody(required = false) SuspendRequest req) {
        return svc.suspend(id, req == null ? null : req.reason());
    }

    @PatchMapping("/{id}/status")
    public StatusResponse updateStatus(@PathVariable UUID id,
                                       @RequestBody StatusChangeRequest req) {
        return svc.updateStatus(id, req.status(), req.reason());
    }

    /** Legacy POST endpoints kept for backwards compatibility. */
    @PostMapping("/{id}/activate")
    public StatusResponse activateLegacy(@PathVariable UUID id) { return svc.activate(id); }
    @PostMapping("/{id}/suspend")
    public StatusResponse suspendLegacy(@PathVariable UUID id,
                                        @RequestBody(required = false) SuspendRequest req) {
        return svc.suspend(id, req == null ? null : req.reason());
    }

    @DeleteMapping("/{id}")
    public StatusResponse delete(@PathVariable UUID id) { return svc.delete(id); }
}
