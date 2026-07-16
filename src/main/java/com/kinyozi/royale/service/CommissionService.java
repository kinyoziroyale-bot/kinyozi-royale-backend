package com.kinyozi.royale.service;

import com.kinyozi.royale.dto.CommissionDtos.*;
import com.kinyozi.royale.model.WorkerCommission;
import com.kinyozi.royale.repository.WorkerCommissionRepository;
import com.kinyozi.royale.service.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode; // 1. Added required import for RoundingMode
import java.util.*;

@Service
public class CommissionService {
    private final WorkerCommissionRepository repo;
    private final JdbcTemplate jdbc;

    @Autowired
    public CommissionService(WorkerCommissionRepository repo, JdbcTemplate jdbc) {
        this.repo = repo;
        this.jdbc = jdbc;
    }

    public List<CommissionResponse> list() {
        UUID t = TenantContext.current();
        List<WorkerCommission> rows = repo.findByTenantId(t);
        if (rows.isEmpty()) return List.of();
        Map<UUID,String> workers = new HashMap<>();
        jdbc.query("select id, full_name from worker where tenant_id = ?",
                ps -> ps.setObject(1, t),
                rs -> { workers.put((UUID) rs.getObject(1), rs.getString(2)); });
        Map<UUID,String> services = new HashMap<>();
        jdbc.query("select id, name from service where tenant_id = ?",
                ps -> ps.setObject(1, t),
                rs -> { services.put((UUID) rs.getObject(1), rs.getString(2)); });
        List<CommissionResponse> out = new ArrayList<>();
        for (WorkerCommission c : rows) {
            CommissionResponse r = new CommissionResponse();
            r.id = c.getId();
            r.workerId = c.getWorkerId();
            r.workerName = workers.getOrDefault(c.getWorkerId(), "—");
            r.serviceId = c.getServiceId();
            r.serviceName = c.getServiceId() == null ? "(All services)" : services.getOrDefault(c.getServiceId(), "—");
            r.percent = c.getPercent();
            r.fixedAmount = c.getFixedAmount();
            r.active = c.isActive();
            out.add(r);
        }
        return out;
    }

    @Transactional
    public WorkerCommission upsert(CommissionRequest req) {
        if (req.workerId == null) throw new IllegalArgumentException("workerId required");
        if (req.percent == null && req.fixedAmount == null)
            throw new IllegalArgumentException("Provide percent or fixedAmount");
        UUID t = TenantContext.current();
        WorkerCommission c = repo.findByTenantIdAndWorkerIdAndServiceId(t, req.workerId, req.serviceId)
                .orElseGet(WorkerCommission::new);
        c.setTenantId(t);
        c.setWorkerId(req.workerId);
        c.setServiceId(req.serviceId);
        c.setPercent(req.percent);
        c.setFixedAmount(req.fixedAmount);
        c.setActive(req.active == null ? true : req.active);
        c.setUpdatedAt(java.time.LocalDateTime.now());
        return repo.save(c);
    }

    @Transactional
    public void delete(UUID id) {
        WorkerCommission c = repo.findById(id).orElseThrow();
        if (!c.getTenantId().equals(TenantContext.current())) throw new SecurityException("Cross-tenant");
        repo.delete(c);
    }

    /**
     * 2. Wrapped the loose snippet into a proper, reusable utility method.
     * Calculates earnings based on a fallback tier strategy (Service-specific vs Base-rate).
     */
    public BigDecimal calculateEarnings(BigDecimal servicePrice, WorkerCommission commission) {
        if (commission == null || !commission.isActive()) {
            return BigDecimal.ZERO;
        }

        // If a specific service override or worker config uses percentage
        if (commission.getPercent() != null) {
            return servicePrice.multiply(commission.getPercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // Fallback to a flat rate setup if assigned
        if (commission.getFixedAmount() != null) {
            return commission.getFixedAmount();
        }

        return BigDecimal.ZERO;
    }
}