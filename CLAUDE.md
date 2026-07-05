# CLAUDE.md — MyLedger

Guidance for AI/dev sessions working in this repo. Read this first.

## What this is
Self-hosted personal-finance app. Started as a house-build expense tracker; grows **by project**
into general finance (expenses, receipts, dashboards, contractor fund requests). Owner-controlled,
security enforced server-side. Deploys on a home server, optionally exposed via Cloudflare Tunnel.

Full project plan: `MyLedger-Project-Plan.md`. In-app docs live at owner route `/owner/docs`
(`frontend/src/owner/Docs.jsx`) — keep it updated when features/schema change.

## Layout
```
backend/            Spring Boot (Java 21, Maven)   com.myledger.{config,controller,service,repository,entity,dto,security}
  src/main/resources/db/migration/  Flyway V1..V7  (Flyway OWNS the schema; JPA runs ddl-auto=validate)
frontend/           React 18 + Vite → nginx        src/{owner,contractor,shared,pages}
docker-compose.yml  postgres + backend + frontend
.env.example        copy to .env (secrets); never commit .env
```

## Build / run / verify — DOCKER ONLY
**This machine has only Docker installed — no local JDK, Maven, Node, or npm.** Do everything through Docker.
- Full stack: `docker compose up -d --build`
- One service: `docker compose build backend` / `docker compose up -d backend` (same for `frontend`)
- Web app: http://localhost:8088 · backend + postgres are NOT published to the host
- Seeded owner: `owner@myledger.local` / `changeme-owner` (compose defaults; override via `.env`)
- Logs: `docker compose logs backend --no-color --tail 50`
- DB shell: `docker exec myledger-db-1 psql -U myledger -d myledger -c "SELECT ..."`
- Health gate before hitting the API:
  `docker inspect --format '{{.State.Health.Status}}' myledger-backend-1` → wait for `healthy`
- After backend Java changes: `docker compose build backend` catches compile errors fast.

## Testing the API from Windows PowerShell (IMPORTANT gotchas)
- `curl.exe` inline JSON like `'{"a":"b"}'` gets its quotes stripped → malformed body → HTTP 400.
  **Fix:** write JSON to a temp file and use `curl.exe --data-binary "@C:\path\body.json"`.
- `$pid` is a read-only PowerShell built-in — use `$projectId` etc.
- PowerShell 5.1 has **no** `??`/`?.`/ternary — a parse error aborts the WHOLE script before running.
- `-o $null` does not suppress the body; write to a temp file and read status via `-w "%{http_code}"`.
- Login → grab token:
  `$T = ([regex]'"accessToken":"([^"]+)"').Match((curl.exe -s -X POST http://localhost:8088/api/auth/login -H "Content-Type: application/json" --data-binary "@login.json")).Groups[1].Value`
  then send `-H "Authorization: Bearer $T"`.
- Generate a BCrypt hash (to insert a user directly): `docker run --rm httpd:2.4-alpine htpasswd -nbBC 10 x "password"`.

## Conventions (follow these)
- **Money is always `BigDecimal` / `NUMERIC(19,4)`** — never float/double. Qty is `NUMERIC(19,3)`.
  Derived totals (fund requests) recompute from line items and `setScale(4, HALF_UP)`.
- **Identity from the JWT only.** Controllers take `@AuthenticationPrincipal AppPrincipal`; never trust a
  userId/role from the request body/param. Contractor ownership is checked in the service layer.
- **Schema changes = a new Flyway migration** (`V{n}__desc.sql`). Do NOT rely on Hibernate to alter tables.
- **Categories and phases are scoped to a project** (project_id FK), like each other. An expense's category
  (and optional phase) must belong to its project — validated in `ExpenseService`.
- **Delete guards:** deleting a project/category/phase/user that is referenced returns **409** ("archive/disable
  instead"). Archive via status/`active` flags. Never hard-delete money data.
- **Auth roles:** `ROLE_OWNER`, `ROLE_CONTRACTOR` (fixed — the security model depends on these two).
  Owner-only paths are matched in `SecurityConfig`; mixed per-method rules use `@PreAuthorize` (method
  security is enabled). Fund-request create/edit is open to any authenticated user; review is owner-only.
- **Errors:** throw `ResponseStatusException(status, msg)` from services. The `/error` dispatch is permitted
  in `SecurityConfig` so real statuses aren't masked as 401 — don't remove that.
- **Frontend money:** `money(value, currencyCode)` (`shared/format.js`). Pass the project's currency where a
  single project's money is shown; omit it for cross-currency views (dashboard stays neutral).
- Match surrounding style: Lombok on entities, records for DTOs, constructor injection, plain fetch in
  `shared/api.js` (in-memory access token + silent refresh; refresh token is an httpOnly cookie).

## Data model (Flyway V1–V7)
`users`, `refresh_tokens` · `projects`(currency) · `categories`(project_id, active, tax_relevant) ·
`phases`(project_id) · `expenses`(BigDecimal, project/category/phase/file) · `files`(receipts) ·
`fund_requests`(status DRAFT→SUBMITTED→APPROVED/REJECTED→PAID, derived total, @Version) · `fund_request_items`.

Migrations: V1 auth · V2 projects/categories/expenses/files · V3 category.active · V4 phases ·
V5 categories-per-project · V6 fund requests · V7 project currency.

## Key config (.env / application.yml)
`DB_*`, `JWT_SECRET`, `OWNER_EMAIL`/`OWNER_PASSWORD`, `COOKIE_SECURE` (true behind HTTPS),
`CORS_ORIGINS` (empty in prod; set to the Vite origin for dev), `FY_START_MONTH` (default 4 = April, Indian FY),
`WEB_PORT` (8088), `FILES_DIR` (/data/files volume).

## Deploy
Production runs on Linux Mint via Docker Compose + a Cloudflare Tunnel (no open ports).
Full guide: `DEPLOY.md`. Overlay: `docker-compose.prod.yml` (adds a `cloudflared` service
using `CLOUDFLARE_TUNNEL_TOKEN`; tunnel public hostname → `http://frontend:80`). Set
`COOKIE_SECURE=true` in prod. Backups: `scripts/backup.sh` (DB dump + files volume, cron).

## Working with the owner
- The owner (sijimonr@gmail.com) actively tests in-browser and creates real accounts/data.
  **Never delete rows that aren't clearly your own test artifacts.** Clean up only what you created.
- After a change, rebuild the affected image, wait for health, then verify end-to-end (login → exercise the
  flow via curl or DB checks), and clean up test data.

## Tabs & contractor scoping (important)
Per-user tab permissions live in `users.tabs` (CSV). Keys in `com.myledger.security.Tabs`:
FUND_REQUESTS, EXPENSES, DASHBOARD, REPORTS. Owners implicitly get all; contractors get their granted set.
Tabs travel in the JWT ("tabs" claim) → `AppPrincipal.canView(tab)`; changing tabs/role revokes refresh tokens.
Frontend contractor nav/routes are driven by `me.tabs` (shared/contractorTabs.js + TabGuard).
- Contractor EXPENSES tab = add/edit/delete their OWN expenses, only for ASSIGNED projects
  (`project_members`, managed on the Users page). Enforced in ExpenseService (owner=all, contractor=own+assigned).
- Contractor DASHBOARD tab = read-only, still ALL projects (not scoped).
- Contractor REPORTS tab = per-project reports scoped to their OWN data in ASSIGNED projects.
- Reference endpoints (`/api/reference/projects[/{id}/categories|phases]`) are role/assignment-aware.

## Status
Milestones done: auth, expenses/dashboard, user management, per-project categories/phases,
financial-year filtering, project currency, contractor+owner fund requests, Docs tab (with architecture +
application-flow diagrams), per-user tab permissions, contractor project assignments + scoped expense entry,
and per-project Reports. Schema at Flyway V9.
Not yet built: contractor receipt upload, PDF export, audit log, automated tests in CI, dashboard scoping for contractors.
Discussed next step: "mark fund request paid → auto-create a matching expense".
