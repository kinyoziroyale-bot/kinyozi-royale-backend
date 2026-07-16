package com.kinyozi.royale.controller;

import com.kinyozi.royale.dto.CustomerDtos.CustomerAnalyticsRow;
import com.kinyozi.royale.dto.CustomerDtos.ReminderRow;
import com.kinyozi.royale.dto.CustomerDtos.UpdateNextVisitRequest;
import com.kinyozi.royale.dto.CustomerRequest;
import com.kinyozi.royale.exception.NotFoundException;
import com.kinyozi.royale.model.Customer;
import com.kinyozi.royale.repository.CustomerRepository;
import com.kinyozi.royale.security.CurrentUser;
import com.kinyozi.royale.service.CustomerAnalyticsService;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Customer endpoints. Uses {@link CustomerRequest} instead of binding the
 * JPA entity so clients cannot mass-assign {@code id}, {@code tenantId},
 * {@code createdAt} or {@code isMainCustomer}. Only OWNER may mark or
 * delete customers.
 */
@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerRepository repo;
    private final CustomerAnalyticsService analytics;
    private final JdbcTemplate jdbc;

    public CustomerController(CustomerRepository r, CustomerAnalyticsService a, JdbcTemplate j) {
        this.repo = r; this.analytics = a; this.jdbc = j;
    }

    @GetMapping
    public List<Customer> list() { return repo.findByTenantId(CurrentUser.tenantId()); }

    @GetMapping("/{id}")
    public Customer get(@PathVariable UUID id) { return mustOwn(id); }

    @PostMapping
    public Customer create(@Valid @RequestBody CustomerRequest body) {
        Customer c = Customer.builder()
                .tenantId(CurrentUser.tenantId())
                .name(body.getName())
                .phone(body.getPhone())
                .nextVisitDate(body.getNextVisitDate())
                .notes(body.getNotes())
                .build();
        return repo.save(c);
    }

    @PutMapping("/{id}")
    public Customer update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest body) {
        Customer e = mustOwn(id);
        // Mutate only whitelisted fields — id, tenantId, createdAt and
        // mainCustomer stay server-owned.
        e.setName(body.getName());
        e.setPhone(body.getPhone());
        if (body.getNextVisitDate() != null) e.setNextVisitDate(body.getNextVisitDate());
        if (body.getNotes() != null) e.setNotes(body.getNotes());
        return repo.save(e);
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        Customer e = mustOwn(id);
        repo.deleteById(e.getId());
    }

    // ---- Analytics ----
    @GetMapping("/analytics")
    public List<CustomerAnalyticsRow> analytics(
            @RequestParam(value = "limit", defaultValue = "200") int limit,
            @RequestParam(value = "mainOnly", defaultValue = "false") boolean mainOnly) {
        return analytics.analytics(limit, mainOnly);
    }

    @GetMapping("/main")
    public List<CustomerAnalyticsRow> mainCustomers(
            @RequestParam(value = "limit", defaultValue = "500") int limit) {
        return analytics.analytics(limit, true);
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/{id}/main")
    public Map<String, Object> markMain(@PathVariable UUID id) {
        UUID tenant = CurrentUser.tenantId();
        int n = jdbc.update(
            "update customers set is_main_customer = true where id = ? and tenant_id = ?",
            id, tenant);
        if (n == 0) throw new NotFoundException("Customer not found");
        return Map.of("id", id, "isMainCustomer", true);
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}/main")
    public Map<String, Object> unmarkMain(@PathVariable UUID id) {
        UUID tenant = CurrentUser.tenantId();
        int n = jdbc.update(
            "update customers set is_main_customer = false where id = ? and tenant_id = ?",
            id, tenant);
        if (n == 0) throw new NotFoundException("Customer not found");
        return Map.of("id", id, "isMainCustomer", false);
    }

    @GetMapping("/reminders")
    public List<ReminderRow> reminders(@RequestParam(value = "bucket", required = false) String bucket) {
        return analytics.reminders(bucket);
    }

    @GetMapping("/reminders/counts")
    public Map<String, Integer> reminderCounts() { return analytics.reminderCounts(); }

    @PatchMapping("/{id}/next-visit")
    public Customer updateNextVisit(@PathVariable UUID id, @RequestBody UpdateNextVisitRequest req) {
        Customer e = mustOwn(id);
        e.setNextVisitDate(req.nextVisitDate);
        if (req.notes != null) e.setNotes(req.notes);
        return repo.save(e);
    }

    private Customer mustOwn(UUID id) {
        Customer e = repo.findById(id).orElseThrow(() -> new NotFoundException("Customer not found"));
        if (!e.getTenantId().equals(CurrentUser.tenantId())) throw new NotFoundException("Customer not found");
        return e;
    }

    @GetMapping("/reminders/summary")
    public Map<String, Integer> reminderSummary() {
        Map<String, Integer> c = analytics.reminderCounts();
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        out.put("today", c.getOrDefault("today", 0));
        out.put("thisWeek", c.getOrDefault("upcoming", 0));
        out.put("overdue", c.getOrDefault("overdue", 0));
        return out;
    }
}
