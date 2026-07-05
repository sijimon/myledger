-- MyLedger V8 — per-user tab permissions. Controls which sections a contractor sees.
-- CSV of tab keys (FUND_REQUESTS, EXPENSES, DASHBOARD). Owners always see everything,
-- so this value is ignored for them. Existing users default to FUND_REQUESTS.

ALTER TABLE users ADD COLUMN tabs VARCHAR(255) NOT NULL DEFAULT 'FUND_REQUESTS';
