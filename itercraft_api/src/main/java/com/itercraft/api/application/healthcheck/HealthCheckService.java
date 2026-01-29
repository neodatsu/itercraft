package com.itercraft.api.application.healthcheck;

import com.itercraft.api.domain.healthcheck.HealthStatus;

public interface HealthCheckService {

    HealthStatus check();
}
