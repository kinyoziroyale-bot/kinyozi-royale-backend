package com.kinyozi.royale.repository;
import com.kinyozi.royale.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
  List<Ticket> findByTenantId(UUID tenantId);
}
