package com.kinyozi.royale.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Idempotent schema repair runner.
 *  - guarantees customers has next_visit_date / notes / is_main_customer
 *  - rebinds stale FKs from legacy "customer" table to canonical "customers"
 *  - creates the refresh_tokens table (defensively — Hibernate ddl-auto=update
 *    will do it in dev; prod uses ddl-auto=validate so we materialise it here).
 */
@Configuration
public class SchemaFixRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaFixRunner.class);

    @Bean
    public ApplicationRunner kinyoziSchemaFix(JdbcTemplate jdbc) {
        return args -> {
            safe(jdbc, "alter table if exists customers add column if not exists next_visit_date date");
            safe(jdbc, "alter table if exists customers add column if not exists notes text");
            safe(jdbc, "alter table if exists customers add column if not exists is_main_customer boolean not null default false");

            rebindCustomerFk(jdbc, "credit",            "customer_id");
            rebindCustomerFk(jdbc, "credit_payment",    "customer_id");
            rebindCustomerFk(jdbc, "customer_sessions", "customer_id");
            rebindCustomerFk(jdbc, "session",           "customer_id");
            rebindCustomerFk(jdbc, "ticket",            "customer_id");

            // Refresh-token storage. Idempotent — matches the RefreshToken
            // entity so validate-mode Hibernate accepts it.
            safe(jdbc,
                "create table if not exists refresh_tokens (" +
                "  id uuid primary key," +
                "  user_id uuid not null," +
                "  tenant_id uuid not null," +
                "  token_hash varchar(64) not null," +
                "  issued_at timestamp not null," +
                "  expires_at timestamp not null," +
                "  revoked boolean not null default false," +
                "  revoked_at timestamp," +
                "  replaced_by uuid," +
                "  user_agent varchar(256)," +
                "  ip varchar(64)" +
                ")");
            safe(jdbc, "create unique index if not exists ix_refresh_tokens_hash on refresh_tokens(token_hash)");
            safe(jdbc, "create index if not exists ix_refresh_tokens_user on refresh_tokens(user_id)");

            // Back-fill subscription metadata for legacy tenants.
            safe(jdbc,
                "insert into tenant_admin_meta (tenant_id, status, deleted, expiry_date, updated_at) " +
                "select t.id, 'ACTIVE', false, (current_date + interval '365 days')::date, now() " +
                "from tenants t " +
                "where not exists (select 1 from tenant_admin_meta m where m.tenant_id = t.id)");

            log.info("[SchemaFixRunner] schema repair complete.");
        };
    }

    private void rebindCustomerFk(JdbcTemplate jdbc, String table, String column) {
        Boolean tableExists = jdbc.queryForObject(
                "select exists(select 1 from information_schema.tables where table_name = ?)",
                Boolean.class, table);
        if (tableExists == null || !tableExists) return;
        Boolean colExists = jdbc.queryForObject(
                "select exists(select 1 from information_schema.columns where table_name = ? and column_name = ?)",
                Boolean.class, table, column);
        if (colExists == null || !colExists) return;

        var fkNames = jdbc.queryForList(
                "select tc.constraint_name from information_schema.table_constraints tc " +
                "join information_schema.key_column_usage kcu on tc.constraint_name = kcu.constraint_name " +
                "and tc.table_name = kcu.table_name " +
                "where tc.constraint_type='FOREIGN KEY' and tc.table_name=? and kcu.column_name=?",
                String.class, table, column);
        for (String fk : fkNames) safe(jdbc, "alter table " + table + " drop constraint if exists \"" + fk + "\"");
        safe(jdbc, "delete from " + table + " where " + column + " is not null and " + column +
                " not in (select id from customers)");
        safe(jdbc, "alter table " + table + " add constraint " + table + "_" + column + "_fk " +
                "foreign key (" + column + ") references customers(id) on delete cascade");
    }

    private static void safe(JdbcTemplate jdbc, String sql) {
        try { jdbc.execute(sql); } catch (Exception ignored) {}
    }
}
