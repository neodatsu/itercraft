package com.itercraft.api.domain.healthcheck;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthStatusTest {

    @Test
    void up_shouldReturnUpStatus() {
        HealthStatus status = HealthStatus.up();

        assertEquals("UP", status.status());
    }
}
