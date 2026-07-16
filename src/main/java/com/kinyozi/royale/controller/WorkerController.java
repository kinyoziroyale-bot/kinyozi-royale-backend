package com.kinyozi.royale.controller;

import com.kinyozi.royale.dto.WorkerCommissionRequest;
import com.kinyozi.royale.dto.WorkerDto;
import com.kinyozi.royale.service.WorkerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workers")
public class WorkerController {

    private final WorkerService svc;
    public WorkerController(WorkerService svc) { this.svc = svc; }

    @GetMapping
    public List<WorkerDto.Response> list(@RequestParam(name = "active", required = false) Boolean activeOnly) {
        return svc.list(activeOnly);
    }

    @GetMapping("/{id}")
    public WorkerDto.Response get(@PathVariable UUID id) { return svc.get(id); }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public WorkerDto.Response create(@Valid @RequestBody WorkerDto.Request r) { return svc.create(r); }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public WorkerDto.Response update(@PathVariable UUID id, @Valid @RequestBody WorkerDto.Request r) {
        return svc.update(id, r);
    }

    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/{id}/active")
    public WorkerDto.Response setActive(@PathVariable UUID id, @RequestParam boolean value) {
        return svc.setActive(id, value);
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        svc.delete(id);
    }

    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/{id}/commission")
    public WorkerDto.Response setCommission(@PathVariable UUID id, @Valid @RequestBody WorkerCommissionRequest req) {
        return svc.updateCommission(id, req);
    }
}
