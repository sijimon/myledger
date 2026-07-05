# MyLedger

Self-hosted personal-finance app. See `MyLedger-Project-Plan.md` for the full plan and
the plan-review refinements. This repository currently implements **M1 — scaffold + auth
end to end**.

## Stack
- **Backend:** Spring Boot 3.3 (Java 21), Spring Security + JWT, Spring Data JPA, Flyway, PostgreSQL
- **Frontend:** React + Vite, served by nginx in production
- **Runtime:** Docker Compose (Postgres + backend + frontend)

## What M1 gives you
- Owner/contractor roles (`ROLE_OWNER`, `ROLE_CONTRACTOR`)
- Login → short-lived JWT access token + revocable, rotating refresh token (httpOnly cookie)
- Server-side role guards; identity resolved from the JWT, never from request params
- Flyway-managed schema (`db/migration/V1__init.sql`: `users`, `refresh_tokens`)
- One-time owner seed on first boot
- Login rate limiting; React login flow with route guards and silent refresh

## Run it (Docker only — no local JDK/Node needed)

```sh
cp .env.example .env        # then edit the secrets
docker compose up --build
```

- Web app: http://localhost:8088 (the `WEB_PORT`)
- Log in with `OWNER_EMAIL` / `OWNER_PASSWORD` from your `.env`
- Postgres is **not** published to the host; only the frontend port is exposed.

### First-login smoke test
1. Open the web app → sign in as the owner → you land on the owner dashboard.
2. Reload the page → the session restores via the refresh cookie (no re-login).
3. Sign out → you're returned to the login screen.

## Development (optional, needs local Node + JDK)
- Backend: `cd backend && ./mvnw spring-boot:run` (or run via Docker).
- Frontend dev server: `cd frontend && npm install && npm run dev` → http://localhost:5173
  (proxies `/api` to `http://localhost:8080`). Set `CORS_ORIGINS=http://localhost:5173`
  if you run the backend outside the proxy.

## Tests
```sh
cd backend && ./mvnw test    # requires a running Docker daemon (Testcontainers-Postgres)
```
`AuthIntegrationTest` verifies login, `/api/me`, 401 on missing/invalid token, and that a
contractor is forbidden (403) from owner-only endpoints.

## Deployment (later — M6)
Point a Cloudflare Tunnel at the published `WEB_PORT`. Set `COOKIE_SECURE=true` and a
strong `JWT_SECRET`. Postgres stays on the internal Docker network.

## Roadmap
M2 expenses + dashboard · M3 invoices + line items + payments · M4 contractor portal ·
M5 PDF export + audit log · M6 deploy. See the project plan.
