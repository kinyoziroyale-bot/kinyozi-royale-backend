package com.kinyozi.royale.controller;

import com.kinyozi.royale.dto.CreditDtos.CreateCreditRequest;
import com.kinyozi.royale.dto.CreditDtos.CreditPaymentRequest;
import com.kinyozi.royale.dto.CreditDtos.CreditResponse;
import com.kinyozi.royale.dto.CreditDtos.PaymentResponse;
import com.kinyozi.royale.service.CreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * NOTE: application.properties sets server.servlet.context-path=/api,
 * so this controller is mapped at "/credits" (final URL => /api/credits).
 * Do NOT use "/api/credits" here or you'll get /api/api/credits.
 */
@RestController
@RequestMapping("/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService service;

    // List all credits for the current tenant (optionally filter by status: OPEN / PAID)
    @GetMapping
    public ResponseEntity<List<CreditResponse>> list(
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(service.list(status));
    }

    // Get a single credit (with payments)
    @GetMapping("/{id}")
    public ResponseEntity<CreditResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    // Create a new credit (e.g. from POS "Save as Credit")
    @PostMapping
    public ResponseEntity<CreditResponse> create(@Valid @RequestBody CreateCreditRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    // Log a payment against an existing credit
    @PostMapping("/{id}/payments")
    public ResponseEntity<PaymentResponse> addPayment(
            @PathVariable UUID id,
            @Valid @RequestBody CreditPaymentRequest req) {
        return ResponseEntity.ok(service.addPayment(id, req));
    }

    // List payments for a credit
    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PaymentResponse>> payments(@PathVariable UUID id) {
        return ResponseEntity.ok(service.payments(id));
    }

    // Delete a credit (tenant-scoped in service).
    // Financial record removal is owner-only to prevent staff from erasing debts.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
