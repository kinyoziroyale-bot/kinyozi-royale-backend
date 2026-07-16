package com.kinyozi.royale.service;

import com.kinyozi.royale.dto.CustomerDtos.CustomerAnalyticsRow;
import com.kinyozi.royale.dto.CustomerDtos.ReminderRow;
import com.kinyozi.royale.security.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

@Service
public class CustomerAnalyticsService {

    private final JdbcTemplate jdbc;
    public CustomerAnalyticsService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Per-customer aggregates from completed sessions. mainOnly filters to admin-curated list. */
    public List<CustomerAnalyticsRow> analytics(int limit, boolean mainOnly) {
        UUID t = CurrentUser.tenantId();
        int cap = limit <= 0 ? 100 : Math.min(limit, 1000);

        String sql =
                "select c.id, c.name, coalesce(c.phone,'') as phone, c.next_visit_date, " +
                "       coalesce(c.is_main_customer, false) as is_main, " +
                "       coalesce(count(distinct s.id) filter (where s.status='COMPLETED'),0) as visits, " +
                "       coalesce(sum(sl.price_charged) filter (where s.status='COMPLETED'),0) as spent, " +
                "       max(s.closed_at) filter (where s.status='COMPLETED') as last_visit " +
                "  from customers c " +
                "  left join customer_sessions s on s.customer_id = c.id and s.tenant_id = c.tenant_id " +
                "  left join session_lines sl on sl.session_id = s.id " +
                " where c.tenant_id = ? " +
                (mainOnly ? " and coalesce(c.is_main_customer,false) = true " : "") +
                " group by c.id, c.name, c.phone, c.next_visit_date, c.is_main_customer " +
                " order by visits desc, spent desc, c.name asc limit ?";

        List<CustomerAnalyticsRow> out = new ArrayList<>();
        jdbc.query(sql,
                ps -> { ps.setObject(1, t); ps.setInt(2, cap); },
                rs -> {
                    CustomerAnalyticsRow r = new CustomerAnalyticsRow();
                    r.id = (UUID) rs.getObject("id");
                    r.name = rs.getString("name");
                    r.phone = rs.getString("phone");
                    r.visits = rs.getLong("visits");
                    BigDecimal spent = rs.getBigDecimal("spent");
                    r.totalSpent = spent == null ? BigDecimal.ZERO : spent;
                    Timestamp ts = rs.getTimestamp("last_visit");
                    r.lastVisit = ts == null ? null : ts.toLocalDateTime();
                    java.sql.Date d = rs.getDate("next_visit_date");
                    r.nextVisitDate = d == null ? null : d.toLocalDate();
                    r.isMainCustomer = rs.getBoolean("is_main");
                    out.add(r);
                });
        return out;
    }

    public List<ReminderRow> reminders(String bucket) {
        UUID t = CurrentUser.tenantId();
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);
        String b = bucket == null ? "ALL" : bucket.trim().toUpperCase();

        StringBuilder sql = new StringBuilder(
                "select id, name, coalesce(phone,'') as phone, next_visit_date " +
                " from customers where tenant_id = ? and next_visit_date is not null ");
        List<Object> params = new ArrayList<>(); params.add(t);
        switch (b) {
            case "TODAY":    sql.append("and next_visit_date = ? "); params.add(today); break;
            case "TOMORROW": sql.append("and next_visit_date = ? "); params.add(today.plusDays(1)); break;
            case "UPCOMING":
            case "WEEK":     sql.append("and next_visit_date between ? and ? "); params.add(today); params.add(weekEnd); break;
            case "OVERDUE":  sql.append("and next_visit_date < ? "); params.add(today); break;
            default: break;
        }
        sql.append("order by next_visit_date asc");

        List<ReminderRow> out = new ArrayList<>();
        jdbc.query(sql.toString(),
                ps -> { for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i)); },
                rs -> {
                    ReminderRow r = new ReminderRow();
                    r.id = (UUID) rs.getObject("id");
                    r.name = rs.getString("name");
                    r.phone = rs.getString("phone");
                    java.sql.Date d = rs.getDate("next_visit_date");
                    r.nextVisitDate = d == null ? null : d.toLocalDate();
                    r.bucket = classify(r.nextVisitDate, today, weekEnd);
                    out.add(r);
                });
        return out;
    }

    public Map<String, Integer> reminderCounts() {
        UUID t = CurrentUser.tenantId();
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);
        Integer todayCount = jdbc.queryForObject(
                "select count(*) from customers where tenant_id=? and next_visit_date=?",
                Integer.class, t, today);
        Integer upcoming = jdbc.queryForObject(
                "select count(*) from customers where tenant_id=? and next_visit_date>? and next_visit_date<=?",
                Integer.class, t, today, weekEnd);
        Integer overdue = jdbc.queryForObject(
                "select count(*) from customers where tenant_id=? and next_visit_date<?",
                Integer.class, t, today);
        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("today", todayCount == null ? 0 : todayCount);
        out.put("upcoming", upcoming == null ? 0 : upcoming);
        out.put("overdue", overdue == null ? 0 : overdue);
        return out;
    }

    private static String classify(LocalDate d, LocalDate today, LocalDate weekEnd) {
        if (d == null) return "NONE";
        if (d.isBefore(today)) return "OVERDUE";
        if (d.equals(today)) return "TODAY";
        if (!d.isAfter(weekEnd)) return "UPCOMING";
        return "LATER";
    }
}
