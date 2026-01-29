package com.itercraft.api.application.healthcheck;

import com.itercraft.api.domain.healthcheck.HealthStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthCheckServiceImplTest {

    private final HealthCheckService service = new HealthCheckServiceImpl();

    @Test
    void check_shouldReturnUpStatus() {
        HealthStatus result = service.check();

        assertEquals("UP", result.status());
    }
}
