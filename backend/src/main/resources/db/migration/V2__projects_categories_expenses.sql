-- MyLedger V2 — projects, categories, expenses, and stored files (receipts).
-- Money is stored as NUMERIC(19,4) everywhere; never floating point.

CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    description TEXT,
    start_date  DATE,
    status      VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE categories (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(80) NOT NULL,
    -- Phase-2 seed: marks categories that matter for tax reports. Unused in Phase 1.
    tax_relevant BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX ux_categories_name ON categories (LOWER(name));

-- Uploaded files (receipts/attachments). Served only through an authenticated endpoint;
-- the raw path is never a public URL.
CREATE TABLE files (
    id            BIGSERIAL PRIMARY KEY,
    storage_key   VARCHAR(80)  NOT NULL,          -- opaque on-disk name (UUID)
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(120) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    uploaded_by   BIGINT       NOT NULL REFERENCES users (id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_files_storage_key ON files (storage_key);

CREATE TABLE expenses (
    id          BIGSERIAL PRIMARY KEY,
    amount      NUMERIC(19, 4) NOT NULL CHECK (amount >= 0),
    expense_date DATE          NOT NULL,
    category_id BIGINT         NOT NULL REFERENCES categories (id),
    project_id  BIGINT         NOT NULL REFERENCES projects (id),
    vendor      VARCHAR(160),
    notes       TEXT,
    file_id     BIGINT         REFERENCES files (id),   -- optional receipt
    created_by  BIGINT         NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX ix_expenses_project ON expenses (project_id);
CREATE INDEX ix_expenses_category ON expenses (category_id);
CREATE INDEX ix_expenses_date ON expenses (expense_date);

-- Seed the first project: the house build.
INSERT INTO projects (name, description, status)
VALUES ('House Build', 'Primary construction project', 'ACTIVE');

-- Seed an initial construction-oriented category set.
INSERT INTO categories (name, tax_relevant) VALUES
    ('Materials', FALSE),
    ('Labor', FALSE),
    ('Permits & Fees', TRUE),
    ('Equipment Rental', FALSE),
    ('Professional Services', TRUE),
    ('Land & Site Work', FALSE),
    ('Utilities', FALSE),
    ('Miscellaneous', FALSE);
