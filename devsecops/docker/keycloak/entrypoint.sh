#!/bin/bash
set -e

REALM_FILE="/opt/keycloak/data/import/itercraft-realm.json"
PASSWORD="${KEYCLOAK_HEALTHCHECK_PASSWORD:-healthcheck}"

sed -i "s/__HEALTHCHECK_PASSWORD__/${PASSWORD}/g" "$REALM_FILE"

exec /opt/keycloak/bin/kc.sh "$@"
