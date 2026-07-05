-- MyLedger V7 — each project has its own currency (ISO 4217 code). Amounts belonging to
-- a project (expenses, fund requests) are shown in that project's currency. Default USD.

ALTER TABLE projects ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'USD';
