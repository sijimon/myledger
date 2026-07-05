/**
 * In-app documentation: architecture, data model, key flows (visual), security, and
 * operations/maintenance. Static reference for the owner.
 */

function Box({ title, sub, accent }) {
  return (
    <div className={`arch-box${accent ? ' accent' : ''}`}>
      <div className="arch-title">{title}</div>
      {sub && <div className="arch-sub">{sub}</div>}
    </div>
  );
}

const ENTITIES = [
  { name: 'users', fields: 'id, email, password_hash, role, enabled', rel: 'owner / contractor accounts' },
  { name: 'refresh_tokens', fields: 'id, user_id→users, token_hash, expires_at, revoked_at', rel: 'revocable sessions' },
  { name: 'projects', fields: 'id, name, status, currency', rel: 'spend area (House Build, Healthcare…)' },
  { name: 'categories', fields: 'id, project_id→projects, name, tax_relevant, active', rel: 'per-project labels' },
  { name: 'phases', fields: 'id, project_id→projects, name, active', rel: 'per-project sub-divisions' },
  { name: 'expenses', fields: 'id, amount, expense_date, project_id, category_id, phase_id, file_id, created_by', rel: 'owner-recorded spend' },
  { name: 'files', fields: 'id, storage_key, content_type, uploaded_by', rel: 'receipts (auth-served)' },
  { name: 'fund_requests', fields: 'id, project_id, requester_id→users, title, status, total, version', rel: 'raised by owner/contractor' },
  { name: 'fund_request_items', fields: 'id, request_id→fund_requests, description, qty, unit_price', rel: 'derive the total' },
];

const MIGRATIONS = [
  ['V1', 'Authentication — users, refresh_tokens'],
  ['V2', 'Projects, categories, expenses, files'],
  ['V3', 'Archivable categories (categories.active)'],
  ['V4', 'Per-project phases'],
  ['V5', 'Categories scoped to a project'],
  ['V6', 'Fund requests + line items'],
  ['V7', 'Per-project currency'],
];

const ENV_VARS = [
  ['DB_NAME / DB_USER / DB_PASSWORD', 'PostgreSQL database + credentials'],
  ['JWT_SECRET', 'Signing secret (hashed to a 256-bit key)'],
  ['OWNER_EMAIL / OWNER_PASSWORD', 'One-time owner seed on first boot'],
  ['COOKIE_SECURE', 'true behind HTTPS (Secure refresh cookie)'],
  ['CORS_ORIGINS', 'SPA origins for the Vite dev server (empty in prod)'],
  ['FY_START_MONTH', 'Financial-year start month (default 4 = April)'],
  ['WEB_PORT', 'Host port the web app is published on (default 8088)'],
];

export default function Docs() {
  return (
    <div className="docs">
      <h2>Application Design &amp; Documentation</h2>
      <p className="muted">
        MyLedger is a self-hosted personal-finance app. It began as a house-build expense
        tracker and grows by project — expenses, receipts, dashboards, and contractor fund
        requests, all owner-controlled and server-enforced.
      </p>

      {/* ---- Architecture ---- */}
      <section className="doc-section">
        <h3>Architecture</h3>
        <p className="muted">Every request flows through one origin; PostgreSQL is never exposed publicly.</p>
        <div className="arch-flow">
          <Box title="Browser — React SPA (Vite)" sub="owner + contractor UI, in-memory access token" />
          <div className="arch-arrow">▼ HTTPS via Cloudflare Tunnel</div>
          <Box title="nginx" sub="serves the SPA · proxies /api (same origin)" />
          <div className="arch-arrow">▼ /api</div>
          <Box title="Spring Boot REST API" sub="Security + JWT · services · Flyway" accent />
          <div className="arch-split">
            <div className="arch-branch">
              <div className="arch-arrow">▼ JDBC</div>
              <Box title="PostgreSQL" sub="internal network only" />
            </div>
            <div className="arch-branch">
              <div className="arch-arrow">▼ disk</div>
              <Box title="File storage" sub="receipts (volume)" />
            </div>
          </div>
        </div>
        <div className="chip-row">
          {['Java 21', 'Spring Boot 3.3', 'Spring Security + JWT', 'Spring Data JPA', 'Flyway', 'PostgreSQL 16',
            'React 18', 'Vite', 'nginx', 'Docker Compose'].map((t) => <span key={t} className="chip">{t}</span>)}
        </div>
      </section>

      {/* ---- Application flow ---- */}
      <section className="doc-section">
        <h3>Application flow</h3>

        <h4>Navigation</h4>
        <p className="muted">On login the JWT carries the role, and the app routes to the right portal.</p>
        <div className="appflow">
          <div className="flow-node accent">Login<span className="arch-sub">identity + role resolved from JWT</span></div>
          <div className="arch-arrow">▼ route guard by role</div>
          <div className="appflow-branch">
            <div className="appflow-col">
              <div className="flow-node actor-o-node">Owner portal</div>
              <div className="arch-arrow">▼</div>
              <div className="flow-boxes">
                {['Dashboard', 'Expenses', 'Fund Requests — review', 'Manage — Projects / Categories / Phases', 'Users', 'Docs']
                  .map((s) => <div key={s} className="flow-mini">{s}</div>)}
              </div>
            </div>
            <div className="appflow-col">
              <div className="flow-node actor-c-node">Contractor portal</div>
              <div className="arch-arrow">▼</div>
              <div className="flow-boxes">
                <div className="flow-mini">Fund Requests — own only</div>
              </div>
            </div>
          </div>
        </div>

        <h4>Data flow</h4>
        <p className="muted">How information moves through the app, end to end.</p>
        <div className="pipeline">
          <div className="pipe-step">
            <div className="pipe-title">1 · Set up</div>
            <div className="muted">Projects (+ currency), categories &amp; phases — under Manage</div>
          </div>
          <span className="flow-arrow">→</span>
          <div className="pipe-step">
            <div className="pipe-title">2 · Record</div>
            <div className="muted">Owner logs expenses &amp; receipts · vendors raise itemised fund requests</div>
          </div>
          <span className="flow-arrow">→</span>
          <div className="pipe-step">
            <div className="pipe-title">3 · Review</div>
            <div className="muted">Owner approves / rejects / marks fund requests paid</div>
          </div>
          <span className="flow-arrow">→</span>
          <div className="pipe-step">
            <div className="pipe-title">4 · Insight</div>
            <div className="muted">Dashboard totals by category / project / phase / month, per financial year</div>
          </div>
        </div>
      </section>

      {/* ---- Roles ---- */}
      <section className="doc-section">
        <h3>Roles &amp; access</h3>
        <div className="grid-2">
          <div className="panel">
            <h4>Owner</h4>
            <ul className="doc-list">
              <li>Full access: expenses, dashboard, projects, categories, phases</li>
              <li>Manages users &amp; roles</li>
              <li>Reviews fund requests (approve / reject / mark paid)</li>
              <li>Can also raise fund requests</li>
            </ul>
          </div>
          <div className="panel">
            <h4>Contractor (vendor)</h4>
            <ul className="doc-list">
              <li>Raises fund requests for a project, with line items</li>
              <li>Sees and edits only their own requests</li>
              <li>No access to expenses, dashboard, or admin</li>
            </ul>
          </div>
        </div>
      </section>

      {/* ---- Feature modules ---- */}
      <section className="doc-section">
        <h3>Feature modules</h3>
        <div className="card-grid">
          {[
            ['Auth & Users', 'JWT login, rotating refresh tokens, owner-managed accounts & roles'],
            ['Projects', 'Spend areas with their own currency; archive-safe'],
            ['Categories & Phases', 'Per-project labels and optional sub-divisions'],
            ['Expenses', 'Amounts (BigDecimal), receipts, vendor, notes'],
            ['Dashboard', 'Totals by category / project / phase / month, filtered by financial year'],
            ['Fund Requests', 'Contractor-raised, itemised, owner-approved workflow'],
          ].map(([t, d]) => (
            <div key={t} className="feature-card"><div className="feature-title">{t}</div><div className="muted">{d}</div></div>
          ))}
        </div>
      </section>

      {/* ---- Data model ---- */}
      <section className="doc-section">
        <h3>Data model</h3>
        <div className="entity-grid">
          {ENTITIES.map((e) => (
            <div key={e.name} className="entity-card">
              <div className="entity-name">{e.name}</div>
              <code className="entity-fields">{e.fields}</code>
              <div className="muted entity-rel">{e.rel}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ---- Flows ---- */}
      <section className="doc-section">
        <h3>Key flows</h3>

        <h4>Authentication</h4>
        <div className="flow">
          {['Login (email + password)', 'API issues JWT + httpOnly refresh cookie',
            'SPA calls /api with Bearer token', 'On 401 → silent refresh (rotates token)', 'Logout revokes refresh token']
            .map((s, i, a) => (
              <span key={s} className="flow-step">{s}{i < a.length - 1 && <span className="flow-arrow">→</span>}</span>
            ))}
        </div>

        <h4>Fund request lifecycle</h4>
        <div className="flow">
          <span className="flow-step actor-c">Contractor/Owner creates <span className="status status-draft">DRAFT</span></span>
          <span className="flow-arrow">→</span>
          <span className="flow-step actor-c">adds line items · submits <span className="status status-submitted">SUBMITTED</span></span>
          <span className="flow-arrow">→</span>
          <span className="flow-step actor-o">Owner <span className="status status-approved">APPROVED</span> / <span className="status status-rejected">REJECTED</span></span>
          <span className="flow-arrow">→</span>
          <span className="flow-step actor-o">Owner <span className="status status-paid">PAID</span></span>
        </div>
        <p className="muted">Totals are always derived from line items. A submitted request is locked from contractor edits; only owners act on it.</p>

        <h4>Expense &amp; dashboard</h4>
        <div className="flow">
          {['Owner records an expense (project, category, phase, receipt)',
            'Stored with BigDecimal precision', 'Dashboard aggregates by financial year', 'Breakdowns by category / project / phase / month']
            .map((s, i, a) => (
              <span key={s} className="flow-step">{s}{i < a.length - 1 && <span className="flow-arrow">→</span>}</span>
            ))}
        </div>
      </section>

      {/* ---- Security ---- */}
      <section className="doc-section">
        <h3>Security model</h3>
        <ul className="doc-list">
          <li>Identity (user id, role) is resolved from the verified JWT — never from a request parameter.</li>
          <li>Role checks are server-side; contractors are scoped to their own data at the service layer.</li>
          <li>Passwords hashed with BCrypt; refresh tokens stored only as SHA-256 hashes and are revocable.</li>
          <li>Login is rate-limited; the refresh cookie is httpOnly + SameSite=Strict (Secure in production).</li>
          <li>Receipts are served through an authenticated endpoint, not public URLs.</li>
          <li>Money is always BigDecimal / NUMERIC(19,4) — never floating point.</li>
        </ul>
      </section>

      {/* ---- Operations ---- */}
      <section className="doc-section">
        <h3>Operations &amp; maintenance</h3>

        <div className="grid-2">
          <div className="panel">
            <h4>Run</h4>
            <pre className="doc-pre">cp .env.example .env   # edit secrets
docker compose up --build

# web: http://localhost:8088
# owner: OWNER_EMAIL / OWNER_PASSWORD</pre>
          </div>
          <div className="panel">
            <h4>Backups</h4>
            <ul className="doc-list">
              <li>Daily <code>pg_dump</code> of PostgreSQL, stored off the server</li>
              <li>Back up the receipts volume alongside the DB</li>
              <li>Rehearse a restore periodically</li>
            </ul>
          </div>
        </div>

        <h4>Schema migrations (Flyway)</h4>
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Version</th><th>Change</th></tr></thead>
            <tbody>
              {MIGRATIONS.map(([v, d]) => (
                <tr key={v}><td><code>{v}</code></td><td>{d}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="muted">Schema is owned by Flyway; JPA runs in <code>validate</code> mode and never mutates it.</p>

        <h4>Configuration (.env)</h4>
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Variable</th><th>Purpose</th></tr></thead>
            <tbody>
              {ENV_VARS.map(([k, d]) => (
                <tr key={k}><td><code>{k}</code></td><td>{d}</td></tr>
              ))}
            </tbody>
          </table>
        </div>

        <h4>Deployment</h4>
        <ul className="doc-list">
          <li>Runs on a home server via Docker Compose.</li>
          <li>Exposed with a Cloudflare Tunnel pointed at the web port — no port forwarding, free TLS.</li>
          <li>Set <code>COOKIE_SECURE=true</code> and a strong <code>JWT_SECRET</code> in production.</li>
        </ul>
      </section>
    </div>
  );
}
