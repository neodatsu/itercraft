FROM eclipse-mosquitto:2.0

# Copy configuration files
COPY mosquitto.conf /mosquitto/config/mosquitto.conf
COPY acl.conf /mosquitto/config/acl.conf

# Create directories for TLS certificates and password file
RUN mkdir -p /mosquitto/config/certs && \
    touch /mosquitto/config/passwd && \
    chown -R mosquitto:mosquitto /mosquitto

# Expose MQTT over TLS (8883) - NO unencrypted port exposed
EXPOSE 8883

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD mosquitto_sub -h localhost -p 8883 --cafile /mosquitto/config/certs/ca.crt \
        -t '$SYS/broker/uptime' -C 1 -W 5 || exit 1

USER mosquitto

CMD ["/usr/sbin/mosquitto", "-c", "/mosquitto/config/mosquitto.conf"]
