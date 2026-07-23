package com.kinyozi.royale.service;

import com.kinyozi.royale.dto.EarningsDtos.BusinessSummary;
import com.kinyozi.royale.dto.EarningsDtos.WorkerEarnings;
import com.kinyozi.royale.model.*;
import com.kinyozi.royale.repository.ServiceItemRepository;
import com.kinyozi.royale.repository.SessionRepository;
import com.kinyozi.royale.repository.WorkerCommissionRepository;
import com.kinyozi.royale.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * NEW COMMISSION MODEL
 * --------------------
 * Commission is defined PER SERVICE (ServiceItem.commissionType +
 * commissionValue). When a session line is completed, that service's
 * commission rule is applied automatically:
 *
 *   PERCENT → priceCharged * (value / 100)
 *   FIXED   → value
 *
 * BACKWARD COMPATIBILITY — resolution order per line:
 *   1. ServiceItem.commissionType / commissionValue                        (NEW default)
 *   2. WorkerCommission row (workerId, serviceId)                          (legacy)
 *   3. WorkerCommission row (workerId, serviceId = null)                   (legacy)
 *   4. Worker.commissionType / commissionValue                             (legacy)
 *   5. else BigDecimal.ZERO
 *
 * Per-worker totals include basic salary (pro-rated to the reporting
 * period) so the frontend can display salary + commission = total.
 * A tenant-wide {@link BusinessSummary} exposes sales, commission,
 * salary and business gross/net profit.
 */
@Service
@Transactional(readOnly = true)
public class EarningsService {
    private static final ThreadLocal<java.math.BigDecimal> TL_UNASSIGNED_SALES = new ThreadLocal<>();
    private static final ThreadLocal<Integer> TL_UNASSIGNED_SERVICES = new ThreadLocal<>();
    private static final ThreadLocal<Integer> TL_UNASSIGNED_SESSIONS = new ThreadLocal<>();

    private final SessionRepository sessions;
    private final WorkerRepository workers;
    private final WorkerCommissionRepository commissions;
    private final ServiceItemRepository services;

    public EarningsService(SessionRepository sessions,
                           WorkerRepository workers,
                           WorkerCommissionRepository commissions,
                           ServiceItemRepository services) {
        this.sessions = sessions;
        this.workers = workers;
        this.commissions = commissions;
        this.services = services;
    }

    // ---------- public endpoints ----------

    public List<WorkerEarnings> daily(LocalDate date) {
        ZoneId z = ZoneId.systemDefault();
        return aggregate(date.atStartOfDay(z).toInstant(),
                date.plusDays(1).atStartOfDay(z).toInstant(), 1);
    }

    public List<WorkerEarnings> range(LocalDate from, LocalDate to) {
        ZoneId z = ZoneId.systemDefault();
        long days = Math.max(1, to.toEpochDay() - from.toEpochDay() + 1);
        return aggregate(from.atStartOfDay(z).toInstant(),
                to.plusDays(1).atStartOfDay(z).toInstant(), days);
    }

    public List<WorkerEarnings> monthly(YearMonth ym) {
        ZoneId z = ZoneId.systemDefault();
        return aggregate(ym.atDay(1).atStartOfDay(z).toInstant(),
                ym.plusMonths(1).atDay(1).atStartOfDay(z).toInstant(),
                ym.lengthOfMonth());
    }

    /** All-time totals — basic salary is NOT pro-rated (would be meaningless). */
    public List<WorkerEarnings> total() {
        return aggregate(Instant.EPOCH, Instant.now().plusSeconds(60), 0);
    }

    public WorkerEarnings forWorker(UUID workerId) {
        return total().stream()
                .filter(w -> w.workerId.equals(workerId))
                .findFirst()
                .orElseGet(() -> {
                    WorkerEarnings e = new WorkerEarnings();
                    e.workerId = workerId;
                    Worker w = workers.findById(workerId).orElse(null);
                    e.workerName = w != null ? w.getFullName() : "Unknown";
                    e.sessions = 0; e.services = 0;
                    e.earnings = e.commission = e.basicSalary =
                            e.totalSales = e.averageEarnings = BigDecimal.ZERO;
                    return e;
                });
    }

    // ---------- business-wide summaries ----------

    public BusinessSummary summaryDaily(LocalDate date) {
        return summaryFor(daily(date));
    }

    public BusinessSummary summaryRange(LocalDate from, LocalDate to) {
        return summaryFor(range(from, to));
    }

    public BusinessSummary summaryMonthly(YearMonth ym) {
        return summaryFor(monthly(ym));
    }

    public BusinessSummary summaryTotal() {
        return summaryFor(total());
    }

    private BusinessSummary summaryFor(List<WorkerEarnings> rows) {
        BusinessSummary s = new BusinessSummary();
        s.totalSales = BigDecimal.ZERO;
        s.totalCommission = BigDecimal.ZERO;
        s.totalSalary = BigDecimal.ZERO;
        s.sessionsCount = 0; s.servicesCount = 0;
        Set<UUID> distinctSessions = new HashSet<>(); // sessions are counted at worker level; sum services below
        for (WorkerEarnings w : rows) {
            s.totalSales      = s.totalSales.add(nz(w.totalSales));
            s.totalCommission = s.totalCommission.add(nz(w.commission));
            s.totalSalary     = s.totalSalary.add(nz(w.basicSalary));
            s.servicesCount  += w.services;
        }
        // Include unassigned-worker lines (revenue only — no commission/salary attributed).
        java.math.BigDecimal unassigned = TL_UNASSIGNED_SALES.get();
        Integer unassignedSvc = TL_UNASSIGNED_SERVICES.get();
        Integer unassignedSess = TL_UNASSIGNED_SESSIONS.get();
        if (unassigned != null) s.totalSales = s.totalSales.add(unassigned);
        if (unassignedSvc != null) s.servicesCount += unassignedSvc;
        if (unassignedSess != null) s.sessionsCount = Math.max(s.sessionsCount, unassignedSess);
        s.totalPayout          = s.totalCommission.add(s.totalSalary);
        s.businessGrossProfit  = s.totalSales.subtract(s.totalCommission);
        s.businessNetProfit    = s.totalSales.subtract(s.totalPayout);
        // sessionsCount: use tenant-level distinct completed sessions
        s.sessionsCount = rows.stream().mapToLong(r -> r.sessions).max().orElse(0);
        return s;
    }

    // ---------- core aggregation ----------

    /**
     * @param periodDays number of calendar days in the reporting period, used
     *                   to pro-rate each worker's basic salary. Pass 0 for
     *                   "unbounded" (all-time) — salary is then omitted.
     */
    private List<WorkerEarnings> aggregate(Instant from, Instant to, long periodDays) {
        UUID tenant = TenantContext.current();
        TL_UNASSIGNED_SALES.set(java.math.BigDecimal.ZERO);
        TL_UNASSIGNED_SERVICES.set(0);
        TL_UNASSIGNED_SESSIONS.set(0);

        // Preload workers, services, and legacy commission rows.
        Map<UUID, Worker> workerById = workers.findByTenantId(tenant).stream()
                .collect(Collectors.toMap(Worker::getId, w -> w));

        Map<UUID, ServiceItem> serviceById = services.findByTenantId(tenant).stream()
                .collect(Collectors.toMap(ServiceItem::getId, s -> s));

        Map<String, WorkerCommission> commissionIndex = new HashMap<>();
        for (WorkerCommission wc : commissions.findByTenantId(tenant)) {
            String key = wc.getWorkerId() + "|"
                    + (wc.getServiceId() == null ? "*" : wc.getServiceId().toString());
            commissionIndex.put(key, wc);
        }

        Map<UUID, WorkerEarnings> map = new HashMap<>();
        Map<UUID, Map<UUID, BigDecimal>> topServicePerWorker = new HashMap<>(); // worker → (service → grossed)
        Map<UUID, Set<UUID>> distinctSessionsPerWorker = new HashMap<>();

        var completed = sessions.findByTenantIdAndStatusAndClosedAtBetween(
                tenant, CustomerSession.Status.COMPLETED, from, to);

        java.math.BigDecimal unassignedSales = java.math.BigDecimal.ZERO;
        int unassignedServices = 0;
        java.util.Set<java.util.UUID> unassignedSessions = new java.util.HashSet<>();
        for (CustomerSession s : completed) {
            for (SessionLine l : s.getLines()) {
                if (l.getWorkerId() == null) {
                    unassignedSales = unassignedSales.add(nz(l.getPriceCharged()));
                    unassignedServices += 1;
                    unassignedSessions.add(s.getId());
                    continue;
                }
                WorkerEarnings e = map.computeIfAbsent(l.getWorkerId(), id -> newRow(id, workerById));
                e.services += 1;
                distinctSessionsPerWorker
                        .computeIfAbsent(l.getWorkerId(), k -> new HashSet<>())
                        .add(s.getId());

                BigDecimal price = nz(l.getPriceCharged());
                e.totalSales = nz(e.totalSales).add(price);

                BigDecimal commission = resolveCommission(l, price, serviceById, commissionIndex, workerById);
                e.commission = nz(e.commission).add(commission);

                ServiceItem svc = serviceById.get(l.getServiceId());
                if (svc != null) {
                    topServicePerWorker
                            .computeIfAbsent(l.getWorkerId(), k -> new HashMap<>())
                            .merge(svc.getId(), price, BigDecimal::add);
                }
            }
        }

        TL_UNASSIGNED_SALES.set(unassignedSales);
        TL_UNASSIGNED_SERVICES.set(unassignedServices);
        TL_UNASSIGNED_SESSIONS.set(unassignedSessions.size());

        // Finalise per-worker rows: pro-rate salary, compute totals + top service.
        for (WorkerEarnings e : map.values()) {
            e.sessions = distinctSessionsPerWorker.getOrDefault(e.workerId, Set.of()).size();

            Worker w = workerById.get(e.workerId);
            e.employmentType = w != null ? w.getEmploymentType() : null;
            e.salaryPeriod   = w != null ? w.getSalaryPeriod() : null;
            e.basicSalary    = proRatedSalary(w, periodDays);

            // totalCommission already accumulated
            boolean paysCommission = w == null
                    || w.getEmploymentType() == null
                    || w.getEmploymentType() != EmploymentType.SALARY_ONLY;
            if (!paysCommission) e.commission = BigDecimal.ZERO;

            e.earnings = nz(e.basicSalary).add(nz(e.commission));
            e.averageEarnings = e.services > 0
                    ? e.earnings.divide(BigDecimal.valueOf(e.services), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Map<UUID, BigDecimal> perSvc = topServicePerWorker.get(e.workerId);
            if (perSvc != null && !perSvc.isEmpty()) {
                UUID top = perSvc.entrySet().stream()
                        .max(Map.Entry.comparingByValue()).get().getKey();
                ServiceItem svc = serviceById.get(top);
                e.topServiceName = svc != null ? svc.getName() : null;
            }
        }

        // Include SALARY_ONLY workers with no services so the payroll shows their salary.
        if (periodDays > 0) {
            for (Worker w : workerById.values()) {
                if (map.containsKey(w.getId())) continue;
                if (w.getEmploymentType() == null || w.getBasicSalary() == null) continue;
                if (w.getEmploymentType() == EmploymentType.COMMISSION_ONLY) continue;
                WorkerEarnings e = newRow(w.getId(), workerById);
                e.basicSalary = proRatedSalary(w, periodDays);
                e.commission = BigDecimal.ZERO;
                e.totalSales = BigDecimal.ZERO;
                e.earnings = nz(e.basicSalary);
                e.averageEarnings = BigDecimal.ZERO;
                e.employmentType = w.getEmploymentType();
                e.salaryPeriod   = w.getSalaryPeriod();
                map.put(w.getId(), e);
            }
        }

        return new ArrayList<>(map.values());
    }

    private WorkerEarnings newRow(UUID id, Map<UUID, Worker> workerById) {
        WorkerEarnings x = new WorkerEarnings();
        x.workerId = id;
        Worker wk = workerById.get(id);
        x.workerName = wk != null ? wk.getFullName() : "Unknown";
        x.earnings = BigDecimal.ZERO;
        x.commission = BigDecimal.ZERO;
        x.basicSalary = BigDecimal.ZERO;
        x.totalSales = BigDecimal.ZERO;
        x.averageEarnings = BigDecimal.ZERO;
        return x;
    }

    private BigDecimal proRatedSalary(Worker w, long periodDays) {
        if (w == null || periodDays <= 0) return BigDecimal.ZERO;
        if (w.getEmploymentType() == null
                || w.getEmploymentType() == EmploymentType.COMMISSION_ONLY) return BigDecimal.ZERO;
        BigDecimal basic = w.getBasicSalary();
        if (basic == null) return BigDecimal.ZERO;
        SalaryPeriod p = w.getSalaryPeriod() == null ? SalaryPeriod.MONTHLY : w.getSalaryPeriod();
        BigDecimal perDay = switch (p) {
            case DAILY   -> basic;
            case WEEKLY  -> basic.divide(BigDecimal.valueOf(7),  6, RoundingMode.HALF_UP);
            case MONTHLY -> basic.divide(BigDecimal.valueOf(30), 6, RoundingMode.HALF_UP);
        };
        return perDay.multiply(BigDecimal.valueOf(periodDays))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveCommission(SessionLine line,
                                         BigDecimal price,
                                         Map<UUID, ServiceItem> serviceById,
                                         Map<String, WorkerCommission> index,
                                         Map<UUID, Worker> workerById) {
        // 1. Per-service rule (NEW default)
        ServiceItem svc = serviceById.get(line.getServiceId());
        if (svc != null && svc.getCommissionType() != null && svc.getCommissionValue() != null) {
            return apply(svc.getCommissionType(), svc.getCommissionValue(), price);
        }
        // 2/3. Legacy per-worker rows
        WorkerCommission wc = index.get(line.getWorkerId() + "|" + line.getServiceId());
        if (wc == null) wc = index.get(line.getWorkerId() + "|*");
        if (wc != null && wc.isActive()) {
            if (wc.getPercent() != null)      return apply(CommissionType.PERCENT, wc.getPercent(), price);
            if (wc.getFixedAmount() != null)  return apply(CommissionType.FIXED,   wc.getFixedAmount(), price);
        }
        // 4. Legacy per-worker default on Worker entity
        Worker w = workerById.get(line.getWorkerId());
        if (w != null && w.getCommissionType() != null && w.getCommissionValue() != null) {
            return apply(w.getCommissionType(), w.getCommissionValue(), price);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal apply(CommissionType type, BigDecimal value, BigDecimal price) {
        if (value == null) return BigDecimal.ZERO;
        return switch (type) {
            case PERCENT -> price.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED   -> value.setScale(2, RoundingMode.HALF_UP);
        };
    }

    private static BigDecimal nz(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }
}
