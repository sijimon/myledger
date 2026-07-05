# Deploying MyLedger on Linux Mint with Cloudflare

This runs the whole app with Docker Compose on your home server (Linux Mint) and exposes
it on the internet through a **Cloudflare Tunnel** — no router port-forwarding, no exposed
home IP, free TLS. PostgreSQL and the backend are never reachable from outside.

```
Browser ──HTTPS──> Cloudflare edge ──tunnel(outbound)──> cloudflared ──> frontend(nginx)
                                                                            └─ /api ─> backend ─> PostgreSQL
```

You need: a domain on Cloudflare (free plan is fine) and this repository on the server.

---

## 1. Install Docker on Linux Mint

Mint is Ubuntu-based. Install Docker Engine + the Compose plugin:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
# Docker's official convenience script:
curl -fsSL https://get.docker.com | sudo sh
# Run docker without sudo (log out/in afterwards):
sudo usermod -aG docker "$USER"
```

Log out and back in, then verify:

```bash
docker version && docker compose version
```

---

## 2. Get the code

```bash
git clone <your-repo-url> myledger    # or copy the folder to the server
cd myledger
```

The Compose project name comes from the folder name (`myledger`), so containers are
`myledger-backend-1`, `myledger-db-1`, etc. (the backup script assumes this).

---

## 3. Configure secrets (`.env`)

```bash
cp .env.example .env
```

Generate strong values and edit `.env`:

```bash
openssl rand -base64 36   # use for JWT_SECRET
openssl rand -base64 24   # use for DB_PASSWORD
openssl rand -base64 18   # use for OWNER_PASSWORD
```

Set at minimum:

```ini
DB_PASSWORD=<generated>
JWT_SECRET=<generated>
OWNER_EMAIL=you@example.com
OWNER_PASSWORD=<generated>
COOKIE_SECURE=true          # REQUIRED in production (served over HTTPS)
CORS_ORIGINS=               # leave empty (SPA is same-origin)
FY_START_MONTH=4            # April = Indian FY; 1 for calendar year
# CLOUDFLARE_TUNNEL_TOKEN=  # filled in step 4
```

`OWNER_EMAIL`/`OWNER_PASSWORD` seed the owner account **only on first boot** (empty DB).
Keep `.env` private — it is git-ignored.

---

## 4. Create the Cloudflare Tunnel

1. In the Cloudflare dashboard open **Zero Trust** → **Networks** → **Tunnels** →
   **Create a tunnel** → **Cloudflared** → name it (e.g. `myledger`).
2. On the "Install connector" screen, **copy the token** (the long string after
   `--token`). Put it in `.env`:
   ```ini
   CLOUDFLARE_TUNNEL_TOKEN=eyJ....(your token)....
   ```
3. Still in the tunnel config, add a **Public Hostname**:
   - **Subdomain / Domain**: e.g. `ledger` . `yourdomain.com`
   - **Type**: `HTTP`
   - **URL**: `frontend:80`   ← the container name, reachable on the compose network
4. Save.

(You don't install cloudflared manually — it runs as a container, see next step.)

---

## 5. Launch

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

This starts `db`, `backend`, `frontend`, and `cloudflared`. Check status:

```bash
docker compose ps
docker compose logs -f cloudflared   # should show "Registered tunnel connection"
```

Open `https://ledger.yourdomain.com` → sign in with `OWNER_EMAIL` / `OWNER_PASSWORD`.
**Change the owner password after first login** (Users tab → Reset password on your row,
or create a second owner and remove the seed one).

To also reach it on the LAN, `http://<server-ip>:8088` still works (that port is only on
your local network; the internet path is the tunnel).

---

## 6. Cloudflare Access (optional edge auth)

The app already has its own login and roles, so Access is optional. If you want an extra
gate at Cloudflare's edge:

- **Zero Trust** → **Access** → **Applications** → **Add a self-hosted application** →
  domain `ledger.yourdomain.com` → add a policy (e.g. Allow, emails ending in your domain,
  or a specific email list).

**Important caveat:** Cloudflare Access sits in front of *everything*, so **every** user —
including contractors — must pass it before reaching the app's own login. Options:
- Add each contractor's email to the Access policy (double login: Cloudflare + app), or
- Skip Access and rely on the app's built-in auth (recommended when you have external
  contractors), or
- Use Access only if this instance is owner-only.

---

## 7. Backups (do this — it's financial data)

A script is included: `scripts/backup.sh` (dumps the DB + receipts volume, keeps the last 14).

```bash
chmod +x scripts/backup.sh
./scripts/backup.sh                       # writes to ~/myledger-backups
```

Schedule daily at 02:00 via cron (`crontab -e`):

```cron
0 2 * * * cd /home/<you>/myledger && ./scripts/backup.sh >> $HOME/myledger-backups/backup.log 2>&1
```

Copy backups off the server periodically (another disk / cloud), and rehearse a restore.

---

## 8. Updating

```bash
cd myledger
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Flyway applies any new migrations automatically on backend startup. Your data persists in
the `db_data` and `files_data` volumes.

---

## 9. Restoring from backup

```bash
# Database (drops into the running db container):
gunzip -c ~/myledger-backups/db-YYYYMMDD-HHMMSS.sql.gz | \
  docker exec -i myledger-db-1 psql -U myledger -d myledger

# Receipts volume:
docker run --rm -v myledger_files_data:/data -v ~/myledger-backups:/backup alpine \
  sh -c "cd /data && tar xzf /backup/files-YYYYMMDD-HHMMSS.tar.gz"
```

For a clean restore, restore into a fresh empty database volume.

---

## 10. Operations & hardening

- **No inbound ports needed.** The tunnel is outbound; you can leave the firewall closed.
  Optionally: `sudo ufw enable` (SSH only).
- **HTTPS everywhere** is handled by Cloudflare; `COOKIE_SECURE=true` makes the refresh
  cookie Secure. The backend trusts `X-Forwarded-*` (proxy-aware) already.
- **Auto-start on boot**: containers use `restart: unless-stopped`, and Docker starts on
  boot — the app comes back after a reboot.
- **Logs**: `docker compose logs -f backend` / `frontend` / `cloudflared`.
- **Stop**: `docker compose -f docker-compose.yml -f docker-compose.prod.yml down`
  (add `-v` only if you intend to delete the database + files — destructive).

---

## Troubleshooting

- **Tunnel not connecting** → `docker compose logs cloudflared`; re-check the token in
  `.env` and that the public hostname URL is exactly `frontend:80`.
- **502 from Cloudflare** → the frontend/backend isn't up yet; `docker compose ps` and
  wait for `backend` to be `healthy`.
- **Login says invalid** → confirm `COOKIE_SECURE=true` and you're using the `https://`
  Cloudflare URL (not plain HTTP), and that you seeded the owner (`docker compose logs
  backend | grep -i seeded`).
- **Change container/volume names** (if you cloned into a different folder): set
  `DB_CONTAINER` / `FILES_VOLUME` when running the backup script.
