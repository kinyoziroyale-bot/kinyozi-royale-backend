-- Feature 2: per-tenant worker-assignment mode. Default preserves current behaviour
-- for every existing tenant.
ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS worker_assignment_mode VARCHAR(32) NOT NULL DEFAULT 'BEFORE_CHECKOUT';

-- Feature 2: allow session lines to be created without a worker.
-- Existing rows keep their worker_id — no data is touched.
ALTER TABLE session_lines
    ALTER COLUMN worker_id DROP NOT NULL;

-- Speed up "sessions with unassigned lines" lookups.
CREATE INDEX IF NOT EXISTS idx_session_lines_worker_null
    ON session_lines (session_id) WHERE worker_id IS NULL;
