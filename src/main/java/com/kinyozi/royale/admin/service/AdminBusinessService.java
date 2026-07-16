package com.kinyozi.royale.admin.service;

import com.kinyozi.royale.admin.dto.AdminDtos.*;
import com.kinyozi.royale.admin.model.TenantAdminMeta;
import com.kinyozi.royale.admin.model.TenantAdminMeta.Status;
import com.kinyozi.royale.admin.repository.TenantAdminMetaRepository;
import com.kinyozi.royale.exception.BadRequestException;
import com.kinyozi.royale.exception.NotFoundException;
import com.kinyozi.royale.model.*;
import com.kinyozi.royale.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kinyozi.royale.admin.security.AdminPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin operations for tenants.
 *
 * PART 1 (Business ID everywhere): {@link BusinessSummary} and
 * {@link BusinessDetail} now include the persistent {@code businessCode}
 * (KR-XXXXXX). Search matches business name, owner name, phone, tenant
 * UUID AND business code so operators can look up a tenant by the code
 * printed on receipts / shared with the owner.
 *
 * PART 3 (Automatic expiry): status returned to the UI is computed from
 * {@link SubscriptionService#computeStatus(TenantAdminMeta)} — an ACTIVE
 * tenant whose expiry date has passed is surfaced as EXPIRED without any
 * admin action or cron job.
 *
 * PART 6 (Dashboard): overview() reports live counts for ACTIVE, TRIAL,
 * EXPIRED and SUSPENDED using the same computed status.
 */
@Service
public class AdminBusinessService {

    private static final Logger log = LoggerFactory.getLogger(AdminBusinessService.class);
    private static String actor() { try { return AdminPrincipal.username(); } catch (Exception e) { return "system"; } }


    private final TenantRepository tenants;
    private final UserRepository users;
    private final WorkerRepository workers;
    private final CustomerRepository customers;
    private final ServiceItemRepository services;
    private final InventoryItemRepository inventory;
    private final CreditRepository credits;
    private final SessionRepository sessions;
    private final TenantAdminMetaRepository meta;
    private final JdbcTemplate jdbc;

    public AdminBusinessService(TenantRepository tenants, UserRepository users,
                                WorkerRepository workers, CustomerRepository customers,
                                ServiceItemRepository services, InventoryItemRepository inventory,
                                CreditRepository credits, SessionRepository sessions,
                                TenantAdminMetaRepository meta, JdbcTemplate jdbc) {
        this.tenants = tenants; this.users = users; this.workers = workers;
        this.customers = customers; this.services = services; this.inventory = inventory;
        this.credits = credits; this.sessions = sessions; this.meta = meta; this.jdbc = jdbc;
    }

    /* ---------------- helpers ---------------- */

    private TenantAdminMeta metaFor(UUID tenantId) {
        return meta.findById(tenantId).orElseGet(() ->
                meta.save(TenantAdminMeta.builder().tenantId(tenantId).build()));
    }

    /** Effective status — takes expiry into account. */
    private Status effectiveStatus(UUID tenantId) {
        return SubscriptionService.computeStatus(meta.findById(tenantId).orElse(null));
    }

    private User ownerOf(UUID tenantId) {
        return users.findAll().stream()
                .filter(u -> tenantId.equals(u.getTenantId()) && u.getRole() == Role.OWNER)
                .findFirst().orElse(null);
    }

    /* ---------------- listing ---------------- */

    public BusinessPage list(String query, String status, int page, int size, boolean showDeleted) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Tenant> all = tenants.findAll();

        List<BusinessSummary> filtered = all.stream()
                .map(t -> {
                    TenantAdminMeta m = meta.findById(t.getId()).orElse(null);
                    Status st = SubscriptionService.computeStatus(m);
                    if (!showDeleted && st == Status.DELETED) return null;
                    if (status != null && !status.isBlank() && !status.equalsIgnoreCase(st.name())) return null;
                    if (!q.isEmpty()) {
                        String hay = ((t.getBusinessName() == null ? "" : t.getBusinessName()) + " " +
                                (t.getBusinessCode() == null ? "" : t.getBusinessCode()) + " " +
                                (t.getOwnerName() == null ? "" : t.getOwnerName()) + " " +
                                (t.getPhone() == null ? "" : t.getPhone()) + " " +
                                t.getId()).toLowerCase(Locale.ROOT);
                        if (!hay.contains(q)) return null;
                    }
                    User owner = ownerOf(t.getId());
                    return new BusinessSummary(
                            t.getId(), t.getBusinessCode(), t.getBusinessName(), t.getOwnerName(),
                            owner == null ? null : owner.getEmail(),
                            t.getPhone(), st.name(), t.getCreatedAt(),
                            workers.findByTenantId(t.getId()).size(),
                            customers.findByTenantId(t.getId()).size(),
                            m == null ? null : m.getSubscriptionPlan(),
                            m == null ? null : m.getExpiryDate(),
                            m == null ? null : m.getLastLoginAt());
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(BusinessSummary::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        long total = filtered.size();
        int from = Math.max(0, Math.min(page * size, filtered.size()));
        int to = Math.max(from, Math.min(from + size, filtered.size()));
        return new BusinessPage(filtered.subList(from, to), total, page, size);
    }

    /* ---------------- detail ---------------- */

    public BusinessDetail detail(UUID tenantId) {
        Tenant t = tenants.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Business not found"));
        TenantAdminMeta m = meta.findById(tenantId).orElse(null);
        Status st = SubscriptionService.computeStatus(m);

        List<User> tenantUsers = users.findAll().stream()
                .filter(u -> tenantId.equals(u.getTenantId())).toList();
        User owner = tenantUsers.stream().filter(u -> u.getRole() == Role.OWNER).findFirst().orElse(null);

        BusinessOwnerDto ownerDto = owner == null ? null
                : new BusinessOwnerDto(owner.getId(), owner.getUsername(),
                        owner.getEmail(), owner.getRole().name(), owner.getCreatedAt());

        List<BusinessOwnerDto> userDtos = tenantUsers.stream()
                .map(u -> new BusinessOwnerDto(u.getId(), u.getUsername(),
                        u.getEmail(), u.getRole().name(), u.getCreatedAt()))
                .toList();

        BusinessStats stats = statsFor(tenantId);
        return new BusinessDetail(
                t.getId(), t.getBusinessCode(), t.getBusinessName(), t.getOwnerName(), t.getPhone(),
                st.name(),
                t.getCreatedAt(),
                m == null ? null : m.getSuspendedAt(),
                m == null ? null : m.getSuspendedReason(),
                m == null ? null : m.getSubscriptionPlan(),
                m == null ? null : m.getExpiryDate(),
                m == null ? null : m.getLastLoginAt(),
                ownerDto, userDtos, stats);
    }

    public BusinessStats statsFor(UUID tenantId) {
        long ws = workers.findByTenantId(tenantId).size();
        long aws = workers.findByTenantIdAndActive(tenantId, true).size();
        long cs = customers.findByTenantId(tenantId).size();
        long ss = services.findByTenantId(tenantId).size();
        long is = inventory.findByTenantId(tenantId).size();

        List<CustomerSession> allSessions = sessions.findByTenantId(tenantId);
        long completed = allSessions.stream().filter(s -> s.getStatus() == CustomerSession.Status.COMPLETED).count();
        long open = allSessions.stream().filter(s -> s.getStatus() == CustomerSession.Status.OPEN).count();

        BigDecimal revenue = jdbc.queryForObject(
                "select coalesce(sum(sl.price_charged),0) from session_lines sl " +
                        "join customer_sessions s on s.id = sl.session_id " +
                        "where s.tenant_id = ? and s.status = 'COMPLETED'",
                BigDecimal.class, tenantId);
        if (revenue == null) revenue = BigDecimal.ZERO;

        BigDecimal creditsOutstanding = credits.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(Credit::balance).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BusinessStats(ws, aws, cs, ss, is,
                allSessions.size(), completed, open, revenue, creditsOutstanding);
    }

    /* ---------------- sub-resources ---------------- */

    public List<WorkerDto> workers(UUID tenantId) {
        return workers.findByTenantId(tenantId).stream()
                .map(w -> new WorkerDto(w.getId(), w.getFullName(), w.getPhoneNumber(),
                        w.getEmail(), w.isActive(), w.getCreatedAt())).toList();
    }
    public List<CustomerDto> customers(UUID tenantId) {
        return customers.findByTenantId(tenantId).stream()
                .map(c -> new CustomerDto(c.getId(), c.getName(), c.getPhone(), c.getCreatedAt())).toList();
    }
    public List<ServiceDto> services(UUID tenantId) {
        return services.findByTenantId(tenantId).stream()
                .map(s -> new ServiceDto(s.getId(), s.getName(), s.getCategory(), s.getPrice(), s.getActive())).toList();
    }
    public List<InventoryDto> inventory(UUID tenantId) {
        return inventory.findByTenantId(tenantId).stream()
                .map(i -> new InventoryDto(i.getId(), i.getName(), i.getCategory(),
                        i.getCurrentQty(), i.getReorderLevel(), i.getPricePerUnit())).toList();
    }
    public List<CreditDto> credits(UUID tenantId) {
        return credits.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(c -> new CreditDto(c.getId(), c.getCustomerId(),
                        c.getTotalOwed(), c.getTotalPaid(), c.balance(),
                        c.getStatus(), c.getNote(),
                        c.getCreatedAt() == null ? null : c.getCreatedAt().toInstant(ZoneOffset.UTC))).toList();
    }
    public List<WorkerEarningDto> workerEarnings(UUID tenantId) {
        Map<UUID, String> names = workers.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Worker::getId, Worker::getFullName, (a, b) -> a));
        return jdbc.query(
                "select sl.worker_id, count(sl.id) as sessions, coalesce(sum(sl.price_charged),0) as gross " +
                        "from session_lines sl " +
                        "join customer_sessions s on s.id = sl.session_id " +
                        "where s.tenant_id = ? and s.status = 'COMPLETED' and sl.worker_id is not null " +
                        "group by sl.worker_id",
                (rs, i) -> {
                    UUID wid = (UUID) rs.getObject("worker_id");
                    return new WorkerEarningDto(wid,
                            names.getOrDefault(wid, "Unknown"),
                            rs.getLong("sessions"), rs.getBigDecimal("gross"));
                }, tenantId);
    }

    /* ---------------- mutations ---------------- */

    @Transactional
    public StatusResponse activate(UUID tenantId) {
        Tenant t = tenants.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Business not found"));
        TenantAdminMeta m = metaFor(t.getId());
        m.setStatus(Status.ACTIVE);
        m.setSuspendedAt(null);
        m.setSuspendedReason(null);
        m.setDeletedAt(null);
        m.setDeleted(false);
        meta.save(m);
        log.info("AUDIT admin-tenant-activate: actor={} tenant={} newStatus={}", actor(), t.getId(), m.getStatus());
        return new StatusResponse(t.getId(), m.getStatus().name(), m.getUpdatedAt());
    }

    @Transactional
    public StatusResponse suspend(UUID tenantId, String reason) {
        Tenant t = tenants.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Business not found"));
        TenantAdminMeta m = metaFor(t.getId());
        m.setStatus(Status.SUSPENDED);
        m.setSuspendedAt(Instant.now());
        m.setSuspendedReason(reason);
        meta.save(m);
        log.info("AUDIT admin-tenant-suspend: actor={} tenant={} newStatus={}", actor(), t.getId(), m.getStatus());
        return new StatusResponse(t.getId(), m.getStatus().name(), m.getUpdatedAt());
    }

    @Transactional
    public StatusResponse updateStatus(UUID tenantId, String statusName, String reason) {
        if (statusName == null) throw new BadRequestException("Status is required");
        Status target;
        try { target = Status.valueOf(statusName.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception e) { throw new BadRequestException("Invalid status: " + statusName); }

        Tenant t = tenants.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Business not found"));
        TenantAdminMeta m = metaFor(t.getId());
        m.setStatus(target);
        switch (target) {
            case ACTIVE, TRIAL -> {
                m.setSuspendedAt(null);
                m.setSuspendedReason(null);
                m.setDeleted(false);
                m.setDeletedAt(null);
            }
            case EXPIRED -> { /* keep any suspension metadata */ }
            case SUSPENDED -> {
                m.setSuspendedAt(Instant.now());
                if (reason != null && !reason.isBlank()) m.setSuspendedReason(reason);
            }
            case DELETED -> {
                m.setDeleted(true);
                m.setDeletedAt(Instant.now());
            }
        }
        meta.save(m);
        log.info("AUDIT admin-tenant-status-change: actor={} tenant={} newStatus={}", actor(), t.getId(), m.getStatus());
        return new StatusResponse(t.getId(), m.getStatus().name(), m.getUpdatedAt());
    }

    /** Soft delete: keep tenant row, hide from default listings. */
    @Transactional
    public StatusResponse delete(UUID tenantId) {
        Tenant t = tenants.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Business not found"));
        TenantAdminMeta m = metaFor(t.getId());
        m.setStatus(Status.DELETED);
        m.setDeleted(true);
        m.setDeletedAt(Instant.now());
        meta.save(m);
        log.info("AUDIT admin-tenant-delete: actor={} tenant={} newStatus={}", actor(), t.getId(), m.getStatus());
        return new StatusResponse(t.getId(), m.getStatus().name(), m.getUpdatedAt());
    }

    /* ---------------- overview & analytics ---------------- */

    public DashboardOverview overview() {
        List<Tenant> all = tenants.findAll();
        long total = all.size();
        // Compute status live so EXPIRED counts include tenants that lapsed today.
        Map<Status, Long> byStatus = all.stream().collect(Collectors.groupingBy(
                t -> effectiveStatus(t.getId()), Collectors.counting()));

        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long todayNew = all.stream().filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().isBefore(startOfToday)).count();
        long monthNew = all.stream().filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().isBefore(startOfMonth)).count();

        long totalUsers = users.count();
        long totalOwners = users.findAll().stream().filter(u -> u.getRole() == Role.OWNER).count();
        long totalWorkers = workers.count();
        long totalCustomers = customers.count();

        Long sessionsToday = jdbc.queryForObject(
                "select count(*) from customer_sessions where opened_at >= ?",
                Long.class, java.sql.Timestamp.from(startOfToday));
        Long sessionsMonth = jdbc.queryForObject(
                "select count(*) from customer_sessions where status = 'COMPLETED' and closed_at >= ?",
                Long.class, java.sql.Timestamp.from(startOfMonth));

        return new DashboardOverview(
                total,
                byStatus.getOrDefault(Status.ACTIVE, 0L),
                byStatus.getOrDefault(Status.SUSPENDED, 0L),
                byStatus.getOrDefault(Status.DELETED, 0L),
                totalOwners, totalUsers, totalWorkers, totalCustomers,
                todayNew, monthNew,
                sessionsToday == null ? 0 : sessionsToday,
                sessionsMonth == null ? 0 : sessionsMonth,
                byStatus.getOrDefault(Status.TRIAL, 0L),
                byStatus.getOrDefault(Status.EXPIRED, 0L));
    }

    public PlatformAnalytics analytics(int days) {
        DashboardOverview o = overview();
        BigDecimal totalRevenue = jdbc.queryForObject(
                "select coalesce(sum(sl.price_charged),0) from session_lines sl " +
                        "join customer_sessions s on s.id = sl.session_id " +
                        "where s.status = 'COMPLETED'", BigDecimal.class);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        Long totalSessions = jdbc.queryForObject("select count(*) from customer_sessions", Long.class);
        Instant from = LocalDate.now(ZoneOffset.UTC).minusDays(days - 1L).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<TimeSeriesPoint> newBiz = jdbc.query(
                "select to_char(date_trunc('day', created_at), 'YYYY-MM-DD') as d, count(*) as c " +
                        "from tenants where created_at >= ? group by 1 order by 1",
                (rs, i) -> new TimeSeriesPoint(rs.getString("d"), rs.getLong("c")),
                java.sql.Timestamp.from(from));
        List<TimeSeriesPoint> sess = jdbc.query(
                "select to_char(date_trunc('day', opened_at), 'YYYY-MM-DD') as d, count(*) as c " +
                        "from customer_sessions where opened_at >= ? group by 1 order by 1",
                (rs, i) -> new TimeSeriesPoint(rs.getString("d"), rs.getLong("c")),
                java.sql.Timestamp.from(from));

        newBiz = fillMissing(newBiz, days);
        sess = fillMissing(sess, days);

        return new PlatformAnalytics(
                o.totalBusinesses(), o.activeBusinesses(), o.suspendedBusinesses(),
                o.totalWorkers(), o.totalCustomers(),
                totalSessions == null ? 0 : totalSessions,
                totalRevenue, newBiz, sess);
    }

    private List<TimeSeriesPoint> fillMissing(List<TimeSeriesPoint> series, int days) {
        Map<String, Long> byDay = series.stream()
                .collect(Collectors.toMap(TimeSeriesPoint::bucket, TimeSeriesPoint::value, (a, b) -> a));
        List<TimeSeriesPoint> out = new ArrayList<>(days);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = days - 1; i >= 0; i--) {
            String key = today.minus(i, ChronoUnit.DAYS).toString();
            out.add(new TimeSeriesPoint(key, byDay.getOrDefault(key, 0L)));
        }
        return out;
    }
}
