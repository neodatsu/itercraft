package com.itercraft.api.infrastructure.web;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResilienceControllerTest {

    private ResilienceController controller;
    private CircuitBreakerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("meteoFrance");
        registry.circuitBreaker("claude");
        controller = new ResilienceController(registry);
    }

    @Test
    void getStatus_shouldReturnAllCircuitBreakers() {
        Map<String, Object> status = controller.getStatus();

        assertThat(status).containsKeys("meteoFrance", "claude");

        @SuppressWarnings("unchecked")
        Map<String, Object> meteoStatus = (Map<String, Object>) status.get("meteoFrance");
        assertThat(meteoStatus).containsKey("state");
        assertThat(meteoStatus).containsKey("failureRate");
        assertThat(meteoStatus).containsKey("bufferedCalls");
    }

    @Test
    void getStatus_shouldShowClosedStateInitially() {
        Map<String, Object> status = controller.getStatus();

        @SuppressWarnings("unchecked")
        Map<String, Object> meteoStatus = (Map<String, Object>) status.get("meteoFrance");
        assertThat(meteoStatus.get("state")).isEqualTo("CLOSED");
    }

    @Test
    void getHealth_shouldReturnUpWhenAllCircuitsAreClosed() {
        Map<String, Object> health = controller.getHealth();

        assertThat(health.get("status")).isEqualTo("UP");

        @SuppressWarnings("unchecked")
        Map<String, String> services = (Map<String, String>) health.get("services");
        assertThat(services.get("meteoFrance")).isEqualTo("HEALTHY");
        assertThat(services.get("claude")).isEqualTo("HEALTHY");
    }

    @Test
    void getHealth_shouldReturnDegradedWhenCircuitIsOpen() {
        // Create a circuit breaker with specific config to force it open
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .failureRateThreshold(50)
                .build();

        CircuitBreakerRegistry testRegistry = CircuitBreakerRegistry.of(config);
        CircuitBreaker cb = testRegistry.circuitBreaker("testService");

        // Force the circuit to open
        cb.transitionToOpenState();

        ResilienceController testController = new ResilienceController(testRegistry);
        Map<String, Object> health = testController.getHealth();

        assertThat(health.get("status")).isEqualTo("DEGRADED");

        @SuppressWarnings("unchecked")
        Map<String, String> services = (Map<String, String>) health.get("services");
        assertThat(services.get("testService")).isEqualTo("UNHEALTHY");
    }

    @Test
    void getHealth_shouldReturnRecoveringWhenCircuitIsHalfOpen() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .failureRateThreshold(50)
                .build();

        CircuitBreakerRegistry testRegistry = CircuitBreakerRegistry.of(config);
        CircuitBreaker cb = testRegistry.circuitBreaker("testService");

        // Force the circuit to half-open
        cb.transitionToOpenState();
        cb.transitionToHalfOpenState();

        ResilienceController testController = new ResilienceController(testRegistry);
        Map<String, Object> health = testController.getHealth();

        assertThat(health.get("status")).isEqualTo("RECOVERING");

        @SuppressWarnings("unchecked")
        Map<String, String> services = (Map<String, String>) health.get("services");
        assertThat(services.get("testService")).isEqualTo("DEGRADED");
    }

    @Test
    void getStatus_shouldTrackCallMetrics() {
        CircuitBreaker cb = registry.circuitBreaker("meteoFrance");

        // Simulate some calls
        cb.onSuccess(100, java.util.concurrent.TimeUnit.MILLISECONDS);
        cb.onSuccess(150, java.util.concurrent.TimeUnit.MILLISECONDS);
        cb.onError(200, java.util.concurrent.TimeUnit.MILLISECONDS, new RuntimeException("Test"));

        Map<String, Object> status = controller.getStatus();

        @SuppressWarnings("unchecked")
        Map<String, Object> meteoStatus = (Map<String, Object>) status.get("meteoFrance");
        assertThat((int) meteoStatus.get("bufferedCalls")).isGreaterThanOrEqualTo(3);
        assertThat((int) meteoStatus.get("successfulCalls")).isEqualTo(2);
        assertThat((int) meteoStatus.get("failedCalls")).isEqualTo(1);
    }
}
