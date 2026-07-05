-- MyLedger V6 — contractor-initiated fund requests against a project, with line items.
-- Only contractors create these (enforced in the API); owners review and act on them.
-- The total is always derived from line items (sum of qty * unit_price), never entered.

CREATE TABLE fund_requests (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT       NOT NULL REFERENCES projects (id),
    requester_id BIGINT       NOT NULL REFERENCES users (id),
    title        VARCHAR(160) NOT NULL,
    note         TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'PAID')),
    total        NUMERIC(19, 4) NOT NULL DEFAULT 0,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_fund_requests_requester ON fund_requests (requester_id);
CREATE INDEX ix_fund_requests_project ON fund_requests (project_id);
CREATE INDEX ix_fund_requests_status ON fund_requests (status);

CREATE TABLE fund_request_items (
    id          BIGSERIAL PRIMARY KEY,
    request_id  BIGINT       NOT NULL REFERENCES fund_requests (id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    qty         NUMERIC(19, 3) NOT NULL CHECK (qty > 0),
    unit_price  NUMERIC(19, 4) NOT NULL CHECK (unit_price >= 0),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_fund_request_items_request ON fund_request_items (request_id);
