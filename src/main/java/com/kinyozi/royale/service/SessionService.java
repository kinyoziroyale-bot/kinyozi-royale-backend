package com.kinyozi.royale.service;

import com.kinyozi.royale.dto.SessionDtos.AddLineRequest;
import com.kinyozi.royale.dto.SessionDtos.LineResponse;
import com.kinyozi.royale.dto.SessionDtos.OpenSessionRequest;
import com.kinyozi.royale.dto.SessionDtos.SessionResponse;
import com.kinyozi.royale.dto.SessionDtos.UpdateLineRequest;
import com.kinyozi.royale.exception.NotFoundException;
import com.kinyozi.royale.model.Customer;
import com.kinyozi.royale.model.CustomerSession;
import com.kinyozi.royale.model.ServiceItem;
import com.kinyozi.royale.model.SessionLine;
import com.kinyozi.royale.repository.CustomerRepository;
import com.kinyozi.royale.repository.ServiceItemRepository;
import com.kinyozi.royale.repository.SessionLineRepository;
import com.kinyozi.royale.repository.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

    private final SessionRepository sessionRepo;
    private final SessionLineRepository lineRepo;
    private final CustomerRepository customerRepo;
    private final ServiceItemRepository serviceRepo;

    public SessionService(SessionRepository sessionRepo,
                          SessionLineRepository lineRepo,
                          CustomerRepository customerRepo,
                          ServiceItemRepository serviceRepo) {
        this.sessionRepo = sessionRepo;
        this.lineRepo = lineRepo;
        this.customerRepo = customerRepo;
        this.serviceRepo = serviceRepo;
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listOpen() {
        return sessionRepo
                .findByTenantIdAndStatus(TenantContext.current(), CustomerSession.Status.OPEN)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listCompletedOnDate(String isoDate) {
        LocalDate day = (isoDate == null || isoDate.isBlank())
                ? LocalDate.now()
                : LocalDate.parse(isoDate);
        ZoneId zone = ZoneId.systemDefault();
        Instant from = day.atStartOfDay(zone).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();
        return sessionRepo.findByTenantIdAndStatusAndClosedAtBetween(
                        TenantContext.current(), CustomerSession.Status.COMPLETED, from, to)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse get(UUID id) {
        return toDto(loadOwned(id));
    }

    @Transactional
    public SessionResponse open(OpenSessionRequest req) {
        UUID tenantId = TenantContext.current();
        Customer customer = customerRepo.findById(req.customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        if (!customer.getTenantId().equals(tenantId)) {
            throw new NotFoundException("Customer not found");
        }
        CustomerSession session = CustomerSession.builder()
                .tenantId(tenantId)
                .customerId(customer.getId())
                .status(CustomerSession.Status.OPEN)
                .openedAt(Instant.now())
                .build();
        return toDto(sessionRepo.save(session));
    }

    @Transactional
    public SessionResponse addLine(UUID sessionId, AddLineRequest req) {
        CustomerSession session = loadOwnedOpen(sessionId);
        // Always trust the server-side service price; never the client.
        BigDecimal price = snapshotPrice(req.serviceId);
        SessionLine line = SessionLine.builder()
                .session(session)
                .serviceId(req.serviceId)
                .workerId(req.workerId)
                .priceCharged(price)
                .startedAt(Instant.now())
                .build();
        session.getLines().add(line);
        return toDto(sessionRepo.save(session));
    }

    @Transactional
    public SessionResponse updateLine(UUID sessionId, UUID lineId, UpdateLineRequest req) {
        CustomerSession session = loadOwnedOpen(sessionId);
        SessionLine line = session.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Line not found"));
        line.setServiceId(req.serviceId);
        line.setWorkerId(req.workerId);
        // Re-snapshot price whenever the service changes; ignore client price.
        line.setPriceCharged(snapshotPrice(req.serviceId));
        return toDto(sessionRepo.save(session));
    }

    @Transactional
    public SessionResponse removeLine(UUID sessionId, UUID lineId) {
        CustomerSession session = loadOwnedOpen(sessionId);
        boolean removed = session.getLines().removeIf(l -> l.getId().equals(lineId));
        if (!removed) throw new NotFoundException("Line not found");
        return toDto(sessionRepo.save(session));
    }

    @Transactional
    public SessionResponse finalize(UUID sessionId) {
        CustomerSession session = loadOwnedOpen(sessionId);
        if (session.getLines().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot finalize an empty session");
        }
        Instant now = Instant.now();
        session.getLines().forEach(l -> { if (l.getEndedAt() == null) l.setEndedAt(now); });
        session.setStatus(CustomerSession.Status.COMPLETED);
        session.setClosedAt(now);
        return toDto(sessionRepo.save(session));
    }

    @Transactional
    public SessionResponse cancel(UUID sessionId) {
        CustomerSession session = loadOwnedOpen(sessionId);
        session.getLines().clear();
        session.setStatus(CustomerSession.Status.VOID);
        session.setClosedAt(Instant.now());
        return toDto(sessionRepo.save(session));
    }

    private CustomerSession loadOwned(UUID id) {
        CustomerSession s = sessionRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Session not found"));
        if (!s.getTenantId().equals(TenantContext.current())) {
            throw new NotFoundException("Session not found");
        }
        return s;
    }

    private CustomerSession loadOwnedOpen(UUID id) {
        CustomerSession s = loadOwned(id);
        if (s.getStatus() != CustomerSession.Status.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is not OPEN");
        }
        return s;
    }

    private BigDecimal snapshotPrice(UUID serviceId) {
        ServiceItem svc = serviceRepo.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found"));
        if (!svc.getTenantId().equals(TenantContext.current())) {
            throw new NotFoundException("Service not found");
        }
        return svc.getPrice() == null ? BigDecimal.ZERO : BigDecimal.valueOf(svc.getPrice());
    }

    private SessionResponse toDto(CustomerSession session) {
        SessionResponse response = new SessionResponse();
        response.id = session.getId();
        response.customerId = session.getCustomerId();
        response.customerName = customerRepo.findById(session.getCustomerId())
                .map(Customer::getName)
                .orElse(null);
        response.status = session.getStatus().name();
        response.openedAt = session.getOpenedAt();
        response.closedAt = session.getClosedAt();
        response.lines = session.getLines().stream().map(this::toLineDto).toList();
        response.total = response.lines.stream()
                .map(line -> line.priceCharged == null ? BigDecimal.ZERO : line.priceCharged)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return response;
    }

    private LineResponse toLineDto(SessionLine line) {
        LineResponse response = new LineResponse();
        response.id = line.getId();
        response.serviceId = line.getServiceId();
        response.workerId = line.getWorkerId();
        response.priceCharged = line.getPriceCharged();
        response.startedAt = line.getStartedAt();
        response.endedAt = line.getEndedAt();
        return response;
    }
}
