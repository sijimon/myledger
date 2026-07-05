-- MyLedger V3 — allow categories to be archived (hidden from new-expense entry while
-- keeping historical expenses intact). Projects already carry a status column.

ALTER TABLE categories ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
