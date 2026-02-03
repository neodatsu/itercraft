#!/bin/bash
# Generate client certificate for an IoT device (for mutual TLS)
# Usage: ./generate-device-cert.sh <device_id>

set -euo pipefail

CERT_DIR="$(dirname "$0")/../certs"
DEVICES_DIR="$CERT_DIR/devices"
DAYS_VALID=365

if [ $# -lt 1 ]; then
    echo "Usage: $0 <device_id>"
    exit 1
fi

DEVICE_ID="$1"
DEVICE_DIR="$DEVICES_DIR/$DEVICE_ID"

if [ ! -f "$CERT_DIR/ca.key" ]; then
    echo "Error: CA certificate not found. Run generate-certs.sh first."
    exit 1
fi

mkdir -p "$DEVICE_DIR"
cd "$CERT_DIR"

echo "=== Generating certificate for device: $DEVICE_ID ==="

openssl genrsa -out "$DEVICE_DIR/client.key" 2048
openssl req -new -key "$DEVICE_DIR/client.key" -out "$DEVICE_DIR/client.csr" \
    -subj "/C=FR/ST=IDF/L=Paris/O=Itercraft/OU=IoT-Device/CN=device-$DEVICE_ID"

openssl x509 -req -in "$DEVICE_DIR/client.csr" -CA ca.crt -CAkey ca.key -CAcreateserial \
    -out "$DEVICE_DIR/client.crt" -days $DAYS_VALID

rm -f "$DEVICE_DIR/client.csr"

# Copy CA cert for device
cp ca.crt "$DEVICE_DIR/"

# Set permissions
chmod 600 "$DEVICE_DIR/client.key"
chmod 644 "$DEVICE_DIR/client.crt" "$DEVICE_DIR/ca.crt"

echo "=== Device certificates generated in $DEVICE_DIR ==="
echo "Files to deploy to device:"
echo "  - ca.crt (CA certificate for server verification)"
echo "  - client.crt (device certificate)"
echo "  - client.key (device private key - KEEP SECRET)"
