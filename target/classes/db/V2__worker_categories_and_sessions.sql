-- Kinyozi Royale — Worker / Category / Session / Earnings schema

CREATE TABLE IF NOT EXISTS worker_categories (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_worker_categories_tenant_name UNIQUE (tenant_id, name)
);
CREATE INDEX IF NOT EXISTS ix_worker_categories_tenant ON worker_categories(tenant_id);

CREATE TABLE IF NOT EXISTS workers (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    full_name     VARCHAR(200) NOT NULL,
    phone_number  VARCHAR(50)  NOT NULL,
    email         VARCHAR(255),
    profile_photo VARCHAR(1000),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_workers_tenant ON workers(tenant_id);

CREATE TABLE IF NOT EXISTS worker_worker_categories (
    worker_id   UUID NOT NULL REFERENCES workers(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES worker_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (worker_id, category_id)
);

-- Many-to-many between EXISTING services table and worker_categories.
CREATE TABLE IF NOT EXISTS service_worker_categories (
    service_id  UUID NOT NULL,
    category_id UUID NOT NULL REFERENCES worker_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (service_id, category_id)
);
-- If your services table has a different name, change the FK target below
-- before running. Comment kept here so you don't miss it.
-- ALTER TABLE service_worker_categories ADD CONSTRAINT fk_swc_service
--   FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS customers (
    id         UUID PRIMARY KEY,
    tenant_id  UUID NOT NULL,
    name       VARCHAR(200) NOT NULL,
    phone      VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_customers_tenant ON customers(tenant_id);

CREATE TABLE IF NOT EXISTS customer_sessions (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL,
    customer_id UUID NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opened_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at   TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_sessions_tenant_status ON customer_sessions(tenant_id, status);
CREATE INDEX IF NOT EXISTS ix_sessions_tenant_closed ON customer_sessions(tenant_id, closed_at);

CREATE TABLE IF NOT EXISTS session_lines (
    id             UUID PRIMARY KEY,
    session_id     UUID NOT NULL REFERENCES customer_sessions(id) ON DELETE CASCADE,
    service_id     UUID NOT NULL,
    worker_id      UUID NOT NULL,
    price_charged  NUMERIC(12,2) NOT NULL,
    started_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at       TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_session_lines_worker ON session_lines(worker_id);
CREATE INDEX IF NOT EXISTS ix_session_lines_session ON session_lines(session_id);
