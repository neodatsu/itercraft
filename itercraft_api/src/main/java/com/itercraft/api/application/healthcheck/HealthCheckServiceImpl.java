package com.itercraft.api.application.healthcheck;

import com.itercraft.api.domain.healthcheck.HealthStatus;
import org.springframework.stereotype.Service;

@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    @Override
    public HealthStatus check() {
        return HealthStatus.up();
    }
}
