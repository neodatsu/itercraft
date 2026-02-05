#!/bin/bash
set -e

REALM_FILE="/opt/keycloak/data/import/itercraft-realm.json"
HEALTHCHECK_PASSWORD="${KEYCLOAK_HEALTHCHECK_PASSWORD:-healthcheck}"
LAURENT_PASSWORD="${KEYCLOAK_LAURENT_PASSWORD:-changeme}"
SSL_REQUIRED="${KEYCLOAK_SSL_REQUIRED:-external}"

sed -i "s/__HEALTHCHECK_PASSWORD__/${HEALTHCHECK_PASSWORD}/g" "$REALM_FILE"
sed -i "s/__LAURENT_PASSWORD__/${LAURENT_PASSWORD}/g" "$REALM_FILE"
sed -i "s/__SSL_REQUIRED__/${SSL_REQUIRED}/g" "$REALM_FILE"

exec /opt/keycloak/bin/kc.sh "$@"
