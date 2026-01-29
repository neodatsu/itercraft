package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.healthcheck.HealthCheckService;
import com.itercraft.api.domain.healthcheck.HealthStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    public HealthCheckController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GetMapping("/healthcheck")
    public ResponseEntity<HealthStatus> healthcheck() {
        return ResponseEntity.ok(healthCheckService.check());
    }
}
