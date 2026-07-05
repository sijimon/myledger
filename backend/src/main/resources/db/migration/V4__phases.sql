-- MyLedger V4 — optional per-project phases (e.g. House Build: Foundation, Framing).
-- An expense may optionally belong to one phase of its project.

CREATE TABLE phases (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT      NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name       VARCHAR(120) NOT NULL,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_phases_project_name ON phases (project_id, LOWER(name));
CREATE INDEX ix_phases_project ON phases (project_id);

ALTER TABLE expenses ADD COLUMN phase_id BIGINT REFERENCES phases (id);
CREATE INDEX ix_expenses_phase ON expenses (phase_id);
