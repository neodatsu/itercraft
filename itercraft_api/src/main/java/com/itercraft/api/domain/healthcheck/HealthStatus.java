package com.itercraft.api.domain.healthcheck;

public record HealthStatus(String status) {

    public static HealthStatus up() {
        return new HealthStatus("UP");
    }
}
