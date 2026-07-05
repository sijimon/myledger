# MyLedger — Project Plan

A self-hosted personal finance application. Phase one tracks expenses, invoices, and contractor payments during a house construction project (1–2 years). Later phases extend it into a general personal finance and tax-keeping system.

---

## 1. Overview

### Purpose
MyLedger is a private, self-hosted web application for tracking money. It begins as a construction-budget tracker with read/write access for third-party contractors, and is designed from day one to grow into a broader personal finance hub (recurring expenses, investments, tax preparation).

### Core principles
- **Self-hosted and private** — runs on the owner's own server, data never leaves it.
- **Owner-first, contractor-scoped** — the owner sees everything; contractors see and edit only their own invoices.
- **Server-enforced security** — every permission check runs server-side from the authentication token, never trusted from the request.
- **Money integrity** — invoice totals are always derived from line items, never hand-entered; paid invoices lock.
- **Built to extend** — the house build is the first *project* inside a general finance app, not a standalone tool.

### Primary users
| Role | Access |
| --- | --- |
| Owner (you) | Full access to all expenses, invoices, payments, dashboards, and projects |
| Contractor | Login account; view and add line items on their own invoices only; view payment status |

---

## 2. Scope

### Phase 1 — House build tracker (MVP)
The first deliverable, used throughout the construction period.

- Authentication and two roles (owner, contractor)
- Expense tracking with categories and receipt attachments
- A "House Build" project that groups construction spend
- Invoice management with line items and status
- Shared line-item editing (both owner and contractor add freely, until an invoice is paid)
- Payment tracking with automatic status updates
- Contractor read/write portal scoped to their own invoices
- Owner dashboard: totals, category breakdown, project spend
- Invoice PDF export
- Audit trail of who added each line item

### Phase 2 — Personal finance extension (later)
Deferred, but the data model accommodates it now.

- Multiple projects / budget areas beyond the house build
- Recurring expenses and income
- Investment and asset tracking
- Tax categorization and year-end reports
- Multi-year rollups and comparisons

### Explicitly out of scope (for now)
- Multi-owner / shared household accounts
- Bank or credit-card feed integrations
- Mobile native apps (the web app is responsive instead)
- Automated tax filing

---

## 3. Architecture

```
React (frontend) ──> Spring Boot REST API ──> PostgreSQL
                          │
                          ├─ Spring Security + JWT (auth + roles)
                          └─ File storage (receipts, invoice PDFs)
```

### Technology stack
| Layer | Choice | Rationale |
| --- | --- | --- |
| Backend | Spring Boot (Java 21) | Matches owner's daily Spring / Jakarta expertise |
| Security | Spring Security + JWT | Role-based access, stateless auth |
| Persistence | Spring Data JPA | Repository abstraction over PostgreSQL |
| Database | PostgreSQL | Reliable, free, strong for financial data |
| Frontend | React (Vite) | Fast dev, componentized owner + contractor views |
| File storage | Local disk (Phase 1) | Simple; can move to object storage later |
| Build | Maven | Familiar multi-module tooling |
| Runtime | Docker Compose | App + Postgres in containers on the home server |

### Deployment target
Runs on the owner's home server. Domain access via **Cloudflare Tunnel** (`cloudflared` makes an outbound connection — no port forwarding, no exposed home IP, works behind CGNAT, free TLS). PostgreSQL stays bound to the local Docker network and is never exposed publicly.

---

## 4. Security model

The most important part of the design. Contractors log in and must only ever touch their own data.

### Roles
- `ROLE_OWNER` — full access
- `ROLE_CONTRACTOR` — scoped to their own invoices

### Enforcement rules
- Contractor identity (`contractor_id`) is always resolved from the JWT, never from a request parameter — this prevents tampering like `?contractorId=5`.
- Every contractor query filters by that resolved id at the data layer, not just in the UI.
- Line-item writes from a contractor pass **two** server-side checks:
  1. **Ownership** — the invoice belongs to the calling contractor (else `403`).
  2. **State** — the invoice is not yet paid (else `409`).
- Payments and expenses are owner-only; contractors have no write path to them.
- Passwords hashed with BCrypt. JWT short-lived, paired with a refresh token.
- Login endpoint rate-limited.
- HTTPS only, HSTS enabled.

### Endpoint access summary
| Endpoint | Owner | Contractor |
| --- | --- | --- |
| `/api/auth/**` | ✅ | ✅ |
| `/api/expenses/**` | ✅ | ❌ |
| `/api/dashboard/**` | ✅ | ❌ |
| `/api/invoices` (list all) | ✅ | ❌ |
| `/api/invoices/mine` | ✅ | ✅ (own) |
| `/api/invoices/{id}` | ✅ | ✅ (own, ownership-checked) |
| `/api/invoices/{id}/items` (write) | ✅ | ✅ (own + not paid) |
| `/api/invoices/{id}/payments` | ✅ | ❌ (view only) |
| `/api/files/upload` | ✅ | ❌ |

---

## 5. Data model

```
users(id, email, password_hash, role, created_at)

contractors(id, user_id FK→users, name, email, phone)

projects(id, name, description, start_date, status)
  # Phase 1 seeds one project: "House Build"

categories(id, name)

expenses(id, amount, date, category_id FK, project_id FK,
         vendor, notes, receipt_url, created_at)

invoices(id, contractor_id FK, project_id FK, invoice_number,
         issue_date, due_date, total, status)
  # total is DERIVED, recomputed on every line-item change
  # status: DRAFT | SENT | PARTIAL | PAID

invoice_items(id, invoice_id FK, description, qty, unit_price,
              created_by FK→users, created_at)

payments(id, invoice_id FK, amount, paid_date, method, created_at)

activity_log(id, invoice_id FK, actor_id FK→users, action, detail, created_at)
```

### Key relationships and rules
- `contractors.user_id` bridges login to invoice scope: contractor logs in → resolve `user.id` → find `contractor.id` → filter invoices.
- `project_id` on expenses and invoices lets spend roll up by project (the house) as well as by category. This is the seam that turns the build tracker into a multi-purpose finance app later.
- `invoices.total` is computed as `sum(qty × unit_price)` across its line items — never entered manually — so the owner's and contractor's numbers can't drift.
- `invoice_items.created_by` records whether the owner or the contractor added each line, for the audit trail.
- `activity_log` gives a per-invoice history ("contractor added 2 items", "owner recorded payment") — valuable for a money app.

---

## 6. API endpoints (Phase 1)

| Method | Endpoint | Role | Purpose |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | all | Authenticate, return JWT |
| POST | `/api/auth/refresh` | all | Refresh access token |
| GET / POST | `/api/expenses` | owner | List / create expenses |
| PUT / DELETE | `/api/expenses/{id}` | owner | Edit / delete an expense |
| GET | `/api/categories` | owner | List categories |
| GET | `/api/projects` | owner | List projects |
| GET | `/api/dashboard/summary` | owner | Totals, category and project breakdown |
| GET / POST | `/api/invoices` | owner | List all / create invoice |
| GET | `/api/invoices/mine` | contractor | Own invoices |
| GET | `/api/invoices/{id}` | both* | Invoice detail (*ownership-checked) |
| POST | `/api/invoices/{id}/items` | both* | Add line item (*own + not paid) |
| PUT / DELETE | `/api/invoices/{id}/items/{itemId}` | both* | Edit / delete line item (*own + not paid) |
| POST | `/api/invoices/{id}/payments` | owner | Record a payment, auto-update status |
| GET | `/api/invoices/{id}/pdf` | both* | Export invoice as PDF |
| POST | `/api/files/upload` | owner | Upload receipt / attachment |

---

## 7. Project structure

```
myledger/
├── docker-compose.yml
├── backend/                      # Spring Boot (Maven)
│   ├── pom.xml
│   └── src/main/java/com/myledger/
│       ├── config/               # SecurityConfig, JWT filter, CORS
│       ├── controller/           # REST controllers
│       ├── service/              # Business logic, total recompute, status rules
│       ├── repository/           # Spring Data JPA repositories
│       ├── entity/               # JPA entities
│       ├── dto/                  # Request / response objects
│       └── security/             # JWT provider, user details, role logic
└── frontend/                     # React (Vite)
    └── src/
        ├── owner/                # Dashboard, expenses, invoices, payments
        ├── contractor/           # Own-invoice list + line-item editing
        ├── shared/               # Auth, API client, route guards
        └── components/           # Reusable UI
```

---

## 8. Build order

A milestone sequence that gets a usable tool in your hands quickly, then layers on the rest.

1. **Scaffold** — Spring Boot project (Web, Security, JPA, PostgreSQL, Validation, Lombok) + Vite React app + Docker Compose with Postgres.
2. **Auth and roles** — users table, login, JWT issue/refresh, BCrypt, role-based route guards on both backend and frontend.
3. **Projects and categories** — seed the "House Build" project and an initial category set.
4. **Expenses** — CRUD, category and project assignment, receipt upload.
5. **Dashboard** — aggregation queries for monthly totals, category breakdown, project spend.
6. **Invoices** — CRUD, line items, derived total, status field.
7. **Shared line-item editing** — the two-gate check (ownership + not paid), `created_by` stamping, total recompute on every change.
8. **Payments** — record payments, auto-update invoice status (`PARTIAL` / `PAID`), lock line items once paid.
9. **Contractor portal** — `/invoices/mine`, own-invoice detail, line-item add/edit within the rules, payment-status view.
10. **Audit trail** — activity log per invoice.
11. **PDF export** — generate invoice PDFs.
12. **Deploy** — containerize, stand up Cloudflare Tunnel, point the domain, enable HTTPS, set up database backups.

---

## 9. Milestones and rough timeline

Assumes part-time evenings/weekends. Adjust to actual availability.

| Milestone | Deliverable | Rough effort |
| --- | --- | --- |
| M1 | Scaffold + auth working end to end | 1–2 weeks |
| M2 | Expenses + dashboard usable (owner can start tracking) | 1–2 weeks |
| M3 | Invoices + shared line items + payments | 2–3 weeks |
| M4 | Contractor portal live | 1–2 weeks |
| M5 | PDF export + audit log | 1 week |
| M6 | Deployed behind domain with backups | 1 week |

**Usable-by point:** after M2 you can already log every construction expense, even before contractors are onboarded.

---

## 10. Operational concerns

- **Backups** — automated daily PostgreSQL dumps; this is financial data you cannot lose. Store copies off the home server.
- **File storage** — receipts and PDFs backed up alongside the database.
- **Database exposure** — Postgres bound to the Docker network / localhost, never published.
- **Secrets** — JWT signing key and DB credentials held outside source control (environment variables / a secrets file).
- **Monitoring** — basic container health checks; log the auth endpoint for suspicious activity.
- **Certificate / TLS** — handled by Cloudflare Tunnel; nothing to renew manually.

---

## 11. Phase 2 extension notes

The Phase 1 model already supports the growth path, so Phase 2 is mostly additive:

- **More projects** — add rows to `projects` (e.g. "Taxes 2027", "Investments"); expenses and invoices already carry `project_id`.
- **Recurring items** — add a `recurrence` definition table that generates expenses/income on a schedule.
- **Income and investments** — new tables that reuse the category and project structure.
- **Tax reports** — tag categories as tax-relevant and add year-end aggregation queries; no structural change needed.
- **Multi-year views** — dashboard queries extended to compare across date ranges.

Because the house build is modeled as a *project* rather than the whole app, none of this requires a rebuild — it slots alongside what already exists.

---

## 12. Open decisions

Items worth confirming before or during the build:

- Invoice numbering scheme (auto-increment vs. per-contractor sequence vs. manual).
- Whether contractors can create their own invoices or only add line items to invoices you create.
- Currency and rounding rules (assume single currency, 2-decimal for now).
- Whether deleting a paid invoice should ever be allowed (recommended: no — archive instead).
- Retention: keep everything indefinitely (recommended for tax history).
