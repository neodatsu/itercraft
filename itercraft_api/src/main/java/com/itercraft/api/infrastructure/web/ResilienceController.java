package com.itercraft.api.infrastructure.web;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller exposing circuit breaker status for monitoring.
 * This endpoint is public and used by the frontend status page.
 */
@RestController
@RequestMapping("/api/resilience")
public class ResilienceController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ResilienceController(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> cbStatus = new HashMap<>();
            cbStatus.put("state", cb.getState().name());
            cbStatus.put("failureRate", cb.getMetrics().getFailureRate());
            cbStatus.put("slowCallRate", cb.getMetrics().getSlowCallRate());
            cbStatus.put("bufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
            cbStatus.put("failedCalls", cb.getMetrics().getNumberOfFailedCalls());
            cbStatus.put("successfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            cbStatus.put("notPermittedCalls", cb.getMetrics().getNumberOfNotPermittedCalls());
            status.put(cb.getName(), cbStatus);
        });

        return status;
    }

    @GetMapping("/health")
    public Map<String, Object> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");

        Map<String, String> services = new HashMap<>();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            CircuitBreaker.State state = cb.getState();
            String serviceStatus = switch (state) {
                case CLOSED -> "HEALTHY";
                case HALF_OPEN -> "DEGRADED";
                case OPEN, DISABLED, FORCED_OPEN -> "UNHEALTHY";
                default -> "UNKNOWN";
            };
            services.put(cb.getName(), serviceStatus);
        });

        health.put("services", services);

        // Overall health is degraded if any circuit is not closed
        boolean allHealthy = services.values().stream().allMatch("HEALTHY"::equals);
        boolean anyUnhealthy = services.values().stream().anyMatch("UNHEALTHY"::equals);

        if (anyUnhealthy) {
            health.put("status", "DEGRADED");
        } else if (!allHealthy) {
            health.put("status", "RECOVERING");
        }

        return health;
    }
}
