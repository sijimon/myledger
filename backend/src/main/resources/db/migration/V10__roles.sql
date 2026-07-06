-- MyLedger V10 — custom roles (named tab presets) assignable to non-owner users.
-- Owner remains the single full-access account type; a role only sets which tabs a
-- contractor-class user sees. Managed under the owner's "Manage" tab.

CREATE TABLE roles (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(80)  NOT NULL,
    tabs       VARCHAR(255) NOT NULL DEFAULT '',
    built_in   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_roles_name ON roles (LOWER(name));

ALTER TABLE users ADD COLUMN role_id BIGINT REFERENCES roles (id);

-- Default role mirroring today's basic contractor: raise fund requests only.
INSERT INTO roles (name, tabs, built_in) VALUES ('Vendor', 'FUND_REQUESTS', TRUE);
