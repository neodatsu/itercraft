#!/bin/bash
set -e

# Start Postgres in the background using the official entrypoint
docker-entrypoint.sh "$@" &
PG_PID=$!

# Wait for Postgres to accept TCP connections
echo "Waiting for PostgreSQL to be ready..."
until pg_isready -h localhost -p 5432 -U "${POSTGRES_USER}" -q; do
  sleep 1
done
echo "PostgreSQL is ready."

# Run Liquibase migrations
echo "Running Liquibase migrations..."
liquibase \
  --changeLogFile=/opt/liquibase/changelog/db.changelog-master.yaml \
  --url="jdbc:postgresql://localhost:5432/${POSTGRES_DB}" \
  --username="${POSTGRES_USER}" \
  --password="${POSTGRES_PASSWORD}" \
  update
echo "Liquibase migrations completed."

# Bring Postgres back to foreground
wait $PG_PID
