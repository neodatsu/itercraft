FROM eclipse-mosquitto:2.0

# Install openssl for certificate generation, su-exec for privilege drop, aws-cli for S3 upload
USER root
RUN apk add --no-cache openssl su-exec aws-cli

# Copy configuration files
COPY mosquitto.conf /mosquitto/config/mosquitto.conf
COPY acl.conf /mosquitto/config/acl.conf
COPY entrypoint.sh /entrypoint.sh

# Create directories and set permissions
RUN mkdir -p /mosquitto/config/certs && \
    touch /mosquitto/config/passwd && \
    chmod +x /entrypoint.sh && \
    chown -R mosquitto:mosquitto /mosquitto

# Expose MQTT over TLS (8883) - NO unencrypted port exposed
EXPOSE 8883

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD mosquitto_sub -h localhost -p 8883 --cafile /mosquitto/config/certs/ca.crt \
        -t '$SYS/broker/uptime' -C 1 -W 5 || exit 1

# Run as root initially, entrypoint will drop to mosquitto user
ENTRYPOINT ["/entrypoint.sh"]
