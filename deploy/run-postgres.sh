#!/usr/bin/env bash
# Simple helper to run a dedicated Postgres container for this project.
# Usage: adjust DB_PORT and DB_PASSWORD, then run `./deploy/run-postgres.sh`

DB_PORT=${DB_PORT:-5432}
DB_PASSWORD=${DB_PASSWORD:-reservas}
CONTAINER_NAME=${CONTAINER_NAME:-reservas-prod-db}
VOLUME_NAME=${VOLUME_NAME:-reservas_pgdata}

docker run -d \
  --name "$CONTAINER_NAME" \
  -e POSTGRES_DB=reservas \
  -e POSTGRES_USER=reservas \
  -e POSTGRES_PASSWORD="$DB_PASSWORD" \
  -p "$DB_PORT":5432 \
  -v "$VOLUME_NAME":/var/lib/postgresql/data \
  --health-cmd "pg_isready -U reservas -d reservas" \
  --health-interval 10s --health-timeout 5s --health-retries 5 \
  postgres:15

echo "Postgres container started: $CONTAINER_NAME (host port $DB_PORT)"

echo "Set these environment variables for your app (example):"
echo "DB_URL=jdbc:postgresql://localhost:${DB_PORT}/reservas"
echo "DB_USERNAME=reservas"
echo "DB_PASSWORD=${DB_PASSWORD}"
