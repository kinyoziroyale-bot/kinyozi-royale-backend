package com.kinyozi.royale.controller;

import com.kinyozi.royale.dto.CommissionDtos.*;
import com.kinyozi.royale.service.CommissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * NOTE: server.servlet.context-path is already "/api", so this controller
 * MUST be mapped at "/commissions" (NOT "/api/commissions"). Otherwise the
 * effective URL becomes /api/api/commissions and the FE gets 404.
 */
/**
 * Commission settings are strictly an owner-level administrative surface.
 * The Role enum has no ADMIN value in the tenant scope — the business owner
 * is the highest tenant role (see {@link com.kinyozi.royale.model.Role}).
 * The previous {@code hasRole('ADMIN')} guard therefore denied every request
 * and made the endpoint unreachable. Correcting to OWNER preserves the
 * intended access model without exposing the endpoint to staff roles.
 */
@RestController
@RequestMapping("/commissions")
@PreAuthorize("hasRole('OWNER')")
public class CommissionController {
    private final CommissionService service;
    public CommissionController(CommissionService service) { this.service = service; }

    @GetMapping
    public List<CommissionResponse> list() { return service.list(); }

    @PostMapping
    public ResponseEntity<?> upsert(@RequestBody CommissionRequest req) {
        return ResponseEntity.ok(service.upsert(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody CommissionRequest req) {
        return ResponseEntity.ok(service.upsert(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
