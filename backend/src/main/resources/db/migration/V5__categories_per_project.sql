-- MyLedger V5 — scope categories to a project (like phases). Each category now belongs
-- to exactly one project; an expense's category must belong to the expense's project.

ALTER TABLE categories ADD COLUMN project_id BIGINT REFERENCES projects (id) ON DELETE CASCADE;

-- Backfill: assign every existing (previously global) category to the seeded House Build
-- project. On the maintainer's live DB, healthcare-specific categories are reassigned
-- afterwards. House Build is guaranteed to exist (seeded in V2).
UPDATE categories
SET project_id = (SELECT id FROM projects WHERE name = 'House Build' ORDER BY id LIMIT 1)
WHERE project_id IS NULL;

ALTER TABLE categories ALTER COLUMN project_id SET NOT NULL;

-- Uniqueness is now per project, not global.
DROP INDEX ux_categories_name;
CREATE UNIQUE INDEX ux_categories_project_name ON categories (project_id, LOWER(name));
CREATE INDEX ix_categories_project ON categories (project_id);
