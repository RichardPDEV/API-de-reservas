#!/usr/bin/env bash
set -Eeuo pipefail

: "${PGHOST:=127.0.0.1}"
: "${PGPORT:=5432}"
: "${PGDATABASE:=reservas}"
: "${PGUSER:=reservas}"
: "${PGPASSWORD:?PGPASSWORD must be provided by the server secret manager}"
: "${BACKUP_DIR:=/var/backups/reservas}"
: "${RETENTION_DAYS:=14}"

umask 077
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date -u +%Y%m%dT%H%M%SZ)
BACKUP_FILE="$BACKUP_DIR/${PGDATABASE}-${TIMESTAMP}.dump"

export PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD
pg_dump --format=custom --file="$BACKUP_FILE"
pg_restore --list "$BACKUP_FILE" >/dev/null

find "$BACKUP_DIR" -maxdepth 1 -type f -name "${PGDATABASE}-*.dump" \
  -mtime "+$RETENTION_DAYS" -delete

printf 'PostgreSQL backup created and verified: %s\n' "$BACKUP_FILE"
