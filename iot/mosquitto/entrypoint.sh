#!/bin/sh
# Entrypoint script for Mosquitto MQTT broker
# Generates TLS certificates on first startup if they don't exist

set -e

CERT_DIR="/mosquitto/config/certs"
DOMAIN="${MQTT_DOMAIN:-mqtt.itercraft.com}"

# Generate certificates if CA doesn't exist
if [ ! -f "$CERT_DIR/ca.crt" ]; then
    echo "=== Generating TLS certificates ==="
    mkdir -p "$CERT_DIR"
    cd "$CERT_DIR"

    # Generate CA
    openssl genrsa -out ca.key 4096
    openssl req -new -x509 -days 3650 -key ca.key -out ca.crt \
        -subj "/C=FR/ST=IDF/L=Paris/O=Itercraft/OU=IoT/CN=Itercraft MQTT CA"

    # Generate server certificate
    openssl genrsa -out server.key 4096
    openssl req -new -key server.key -out server.csr \
        -subj "/C=FR/ST=IDF/L=Paris/O=Itercraft/OU=IoT/CN=$DOMAIN"

    # Create SAN extension
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
        -out server.crt -days 365 -extfile server.ext

    rm -f server.csr server.ext

    # Set permissions for mosquitto user
    chmod 600 *.key
    chmod 644 *.crt
    chown -R mosquitto:mosquitto "$CERT_DIR"

    echo "=== Certificates generated for $DOMAIN ==="
fi

# Create empty passwd file if it doesn't exist
if [ ! -f "/mosquitto/config/passwd" ]; then
    echo "=== Creating empty password file ==="
    touch /mosquitto/config/passwd
    chown mosquitto:mosquitto /mosquitto/config/passwd
fi

# Ensure correct ownership
chown -R mosquitto:mosquitto /mosquitto

echo "=== Starting Mosquitto ==="
exec su-exec mosquitto /usr/sbin/mosquitto -c /mosquitto/config/mosquitto.conf
