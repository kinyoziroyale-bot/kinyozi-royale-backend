package com.kinyozi.royale.controller;
import com.kinyozi.royale.exception.NotFoundException;
import com.kinyozi.royale.model.Ticket;
import com.kinyozi.royale.repository.TicketRepository;
import com.kinyozi.royale.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Ticket endpoints. Uses an explicit request DTO ({@link TicketRequest})
 * instead of binding the JPA entity directly, so clients cannot mass-assign
 * server-owned fields such as {@code tenantId}, {@code id} or {@code openedAt}.
 * Deletion is a financial mutation and is restricted to the business owner.
 */
@RestController @RequestMapping("/tickets")
public class TicketController {
  private final TicketRepository repo;
  public TicketController(TicketRepository r){ this.repo = r; }

  public record TicketRequest(
      UUID customerId,
      @Size(max = 200) String customerName,
      UUID workerId,
      @Size(max = 200) String workerName,
      @NotBlank @Pattern(regexp = "OPEN|COMPLETED|VOID",
          message = "status must be OPEN, COMPLETED or VOID") String status,
      @PositiveOrZero Integer total
  ) {}

  @GetMapping public List<Ticket> list(){ return repo.findByTenantId(CurrentUser.tenantId()); }

  @GetMapping("/{id}") public Ticket get(@PathVariable UUID id){
    Ticket e = repo.findById(id).orElseThrow(() -> new NotFoundException("Not found"));
    if (!e.getTenantId().equals(CurrentUser.tenantId())) throw new NotFoundException("Not found");
    return e;
  }

  @PostMapping public Ticket create(@Valid @RequestBody TicketRequest body){
    Ticket t = Ticket.builder()
        .tenantId(CurrentUser.tenantId())
        .customerId(body.customerId())
        .customerName(body.customerName())
        .workerId(body.workerId())
        .workerName(body.workerName())
        .status(body.status() == null ? "OPEN" : body.status())
        .total(body.total() == null ? 0 : body.total())
        .openedAt(Instant.now())
        .build();
    return repo.save(t);
  }

  @PutMapping("/{id}") public Ticket update(@PathVariable UUID id, @Valid @RequestBody TicketRequest body){
    Ticket e = repo.findById(id).orElseThrow(() -> new NotFoundException("Not found"));
    if (!e.getTenantId().equals(CurrentUser.tenantId())) throw new NotFoundException("Not found");
    // Mutate only whitelisted fields — tenantId, id and openedAt are server-owned.
    e.setCustomerId(body.customerId());
    e.setCustomerName(body.customerName());
    e.setWorkerId(body.workerId());
    e.setWorkerName(body.workerName());
    if (body.status() != null) e.setStatus(body.status());
    if (body.total() != null) e.setTotal(body.total());
    if ("COMPLETED".equals(e.getStatus()) && e.getClosedAt() == null) {
      e.setClosedAt(Instant.now());
    }
    return repo.save(e);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('OWNER')")
  public void delete(@PathVariable UUID id){
    Ticket e = repo.findById(id).orElseThrow(() -> new NotFoundException("Not found"));
    if (!e.getTenantId().equals(CurrentUser.tenantId())) throw new NotFoundException("Not found");
    repo.deleteById(id);
  }
}
