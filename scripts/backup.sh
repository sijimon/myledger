#!/usr/bin/env bash
# MyLedger backup: dumps the PostgreSQL database and the receipts volume.
# Usage:  ./scripts/backup.sh [DEST_DIR]   (default: ~/myledger-backups)
# Cron:   0 2 * * * cd /path/to/myledger && ./scripts/backup.sh >> ~/myledger-backups/backup.log 2>&1
set -euo pipefail

# Load DB_USER / DB_NAME from .env if present.
cd "$(dirname "$0")/.."
set -a; [ -f .env ] && . ./.env; set +a
DB_USER="${DB_USER:-myledger}"
DB_NAME="${DB_NAME:-myledger}"

# Compose project name defaults to the folder name → container / volume prefixes.
PROJECT="$(basename "$(pwd)")"
DB_CONTAINER="${DB_CONTAINER:-${PROJECT}-db-1}"
FILES_VOLUME="${FILES_VOLUME:-${PROJECT}_files_data}"

DEST="${1:-$HOME/myledger-backups}"
STAMP="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$DEST"

echo "[$(date)] backing up db=$DB_NAME container=$DB_CONTAINER volume=$FILES_VOLUME -> $DEST"

# Database (gzipped SQL dump)
docker exec "$DB_CONTAINER" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$DEST/db-$STAMP.sql.gz"

# Receipts/attachments volume (tarball)
docker run --rm -v "$FILES_VOLUME":/data:ro -v "$DEST":/backup alpine \
  tar czf "/backup/files-$STAMP.tar.gz" -C /data .

# Retention: keep the most recent 14 of each.
ls -1t "$DEST"/db-*.sql.gz    2>/dev/null | tail -n +15 | xargs -r rm -f
ls -1t "$DEST"/files-*.tar.gz 2>/dev/null | tail -n +15 | xargs -r rm -f

echo "[$(date)] backup complete: db-$STAMP.sql.gz, files-$STAMP.tar.gz"
