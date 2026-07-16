package com.kinyozi.royale.controller;

import com.kinyozi.royale.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

/**
 * Read-only enriched view: credits joined with customers + payments.
 * Returns an empty list (never 500) so the FE always renders a clean table.
 *
 * Endpoint: GET /credits/view?q=...
 *
 * FIX: The `credit` table does NOT have a physical `balance` column
 * (Credit.balance() is computed in Java). Selecting `cr.balance` therefore
 * caused: `PreparedStatementCallback; bad SQL grammar ... column cr.balance
 * does not exist`, which made the Credit Tracking page appear empty.
 * We now compute the balance in SQL as (total_owed - coalesce(total_paid, 0)).
 */
@RestController
@RequestMapping("/credits/view")
public class CreditViewController {

    private static final Logger log = LoggerFactory.getLogger(CreditViewController.class);
    private final JdbcTemplate jdbc;
    public CreditViewController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(value = "q", required = false) String q) {
        UUID t = CurrentUser.tenantId();
        String creditTable = resolveCreditTable();
        if (creditTable == null) return List.of();

        String like = (q == null || q.isBlank()) ? null : "%" + q.toLowerCase() + "%";
        String sql =
            "select cr.id, cr.customer_id, cr.session_id, " +
            "       coalesce(cr.total_owed, 0) as total_owed, " +
            "       coalesce(cr.total_paid, 0) as total_paid, " +
            "       (coalesce(cr.total_owed, 0) - coalesce(cr.total_paid, 0)) as balance, " +
            "       cr.status, cr.note, cr.created_at, " +
            "       c.name as customer_name, coalesce(c.phone,'') as customer_phone " +
            "  from " + creditTable + " cr " +
            "  left join customers c on c.id = cr.customer_id " +
            " where cr.tenant_id = ? " +
            (like != null
                ? " and (lower(coalesce(c.name,'')) like ? or lower(coalesce(c.phone,'')) like ? " +
                  "      or lower(coalesce(cr.note,'')) like ?) "
                : "") +
            " order by cr.created_at desc";

        List<Map<String, Object>> credits;
        try {
            credits = jdbc.query(sql, ps -> {
                ps.setObject(1, t);
                if (like != null) { ps.setString(2, like); ps.setString(3, like); ps.setString(4, like); }
            }, (rs, i) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getObject("id"));
                m.put("customerId", rs.getObject("customer_id"));
                m.put("sessionId", rs.getObject("session_id"));
                m.put("customerName", rs.getString("customer_name"));
                m.put("customerPhone", rs.getString("customer_phone"));
                m.put("totalOwed", nz(rs.getBigDecimal("total_owed")));
                m.put("totalPaid", nz(rs.getBigDecimal("total_paid")));
                m.put("balance",   nz(rs.getBigDecimal("balance")));
                m.put("status", rs.getString("status"));
                m.put("note", rs.getString("note"));
                Timestamp ts = rs.getTimestamp("created_at");
                m.put("createdAt", ts == null ? null : ts.toInstant().toString());
                m.put("payments", new ArrayList<>());
                return m;
            });
        } catch (Exception ex) {
            log.warn("[CreditView] list query failed: {}", ex.getMessage());
            return List.of();
        }

        if (credits.isEmpty()) return credits;

        try {
            List<UUID> ids = credits.stream().map(c -> (UUID) c.get("id")).toList();
            String inSql = String.join(",", Collections.nCopies(ids.size(), "?"));
            String paySql = "select id, credit_id, amount, paid_at, note, created_by " +
                            "  from credit_payment where credit_id in (" + inSql + ") order by paid_at asc";
            Map<UUID, List<Map<String, Object>>> byCredit = new HashMap<>();
            jdbc.query(paySql,
                ps -> { for (int i = 0; i < ids.size(); i++) ps.setObject(i + 1, ids.get(i)); },
                rs -> {
                    UUID cid = (UUID) rs.getObject("credit_id");
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("id", rs.getObject("id"));
                    p.put("amount", nz(rs.getBigDecimal("amount")));
                    Timestamp t2 = rs.getTimestamp("paid_at");
                    p.put("paidAt", t2 == null ? null : t2.toInstant().toString());
                    p.put("note", rs.getString("note"));
                    p.put("createdBy", rs.getString("created_by"));
                    byCredit.computeIfAbsent(cid, k -> new ArrayList<>()).add(p);
                });
            for (Map<String, Object> c : credits) {
                List<Map<String, Object>> ps = byCredit.get((UUID) c.get("id"));
                if (ps != null) c.put("payments", ps);
            }
        } catch (Exception ex) {
            log.warn("[CreditView] payments query failed: {}", ex.getMessage());
        }
        return credits;
    }

    /** Detect the actual credit table name (some databases use "credit", others "credits"). */
    private String resolveCreditTable() {
        for (String name : new String[]{"credit", "credits"}) {
            Boolean ok = jdbc.queryForObject(
                "select exists(select 1 from information_schema.tables where table_name = ?)",
                Boolean.class, name);
            if (Boolean.TRUE.equals(ok)) return name;
        }
        return null;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
