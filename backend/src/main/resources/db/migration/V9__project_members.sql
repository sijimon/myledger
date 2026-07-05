-- MyLedger V9 — contractor↔project assignments. A contractor assigned to a project may
-- add/manage their own expenses for that project (enforced in the API).

CREATE TABLE project_members (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT      NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (project_id, user_id)
);

CREATE INDEX ix_project_members_user ON project_members (user_id);
