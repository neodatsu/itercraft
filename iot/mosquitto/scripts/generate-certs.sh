#!/bin/bash
# Generate TLS certificates for Mosquitto MQTT broker
# Run this script once to generate CA and server certificates

set -euo pipefail

CERT_DIR="$(dirname "$0")/../certs"
DOMAIN="${DOMAIN:-mqtt.itercraft.com}"
DAYS_VALID=365
CA_DAYS_VALID=3650

mkdir -p "$CERT_DIR"
cd "$CERT_DIR"

echo "=== Generating CA certificate ==="
openssl genrsa -out ca.key 4096
openssl req -new -x509 -days $CA_DAYS_VALID -key ca.key -out ca.crt \
    -subj "/C=FR/ST=IDF/L=Paris/O=Itercraft/OU=IoT/CN=Itercraft MQTT CA"

echo "=== Generating server certificate ==="
openssl genrsa -out server.key 4096
openssl req -new -key server.key -out server.csr \
    -subj "/C=FR/ST=IDF/L=Paris/O=Itercraft/OU=IoT/CN=$DOMAIN"

# Create SAN extension file
cat > server.ext << EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = $DOMAIN
DNS.2 = localhost
EOF

openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
    -out server.crt -days $DAYS_VALID -extfile server.ext

rm -f server.csr server.ext

# Set proper permissions
chmod 600 *.key
chmod 644 *.crt

echo "=== Certificates generated in $CERT_DIR ==="
echo "CA Certificate: ca.crt"
echo "Server Certificate: server.crt"
echo "Server Key: server.key"
echo ""
echo "Distribute ca.crt to IoT devices for TLS verification"
