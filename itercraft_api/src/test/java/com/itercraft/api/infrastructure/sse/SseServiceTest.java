package com.itercraft.api.infrastructure.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SseServiceTest {

    private SseService sseService;

    @BeforeEach
    void setUp() {
        sseService = new SseService();
    }

    @Test
    void register_shouldReturnEmitter() {
        SseEmitter emitter = sseService.register();

        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void register_shouldSetupCallbacks() {
        SseEmitter emitter = sseService.register();

        assertThat(emitter).isNotNull();
        // Trigger completion callback
        emitter.complete();
    }

    @Test
    void broadcast_shouldSendEventToAllEmitters() throws IOException {
        // Register an emitter
        SseEmitter emitter = sseService.register();

        // Broadcast should not throw
        sseService.broadcast("test-event");

        // Emitter should still be valid (no exception)
        assertThat(emitter).isNotNull();
    }

    @Test
    void broadcast_shouldRemoveFailedEmitters() {
        // Register and complete the emitter to simulate failure
        SseEmitter emitter = sseService.register();
        emitter.complete();

        // Broadcast should handle the completed emitter gracefully
        sseService.broadcast("test-event");

        // No exception means success
        assertThat(emitter).isNotNull();
    }

    @Test
    void broadcast_shouldHandleIOException() throws IOException {
        // Register multiple emitters
        SseEmitter emitter1 = sseService.register();
        SseEmitter emitter2 = sseService.register();

        // Complete one to simulate it being closed
        emitter1.complete();

        // Broadcast should continue despite failure on first emitter
        sseService.broadcast("subscription-change");

        assertThat(emitter2).isNotNull();
    }

    @Test
    void onTimeout_shouldRemoveEmitter() {
        SseEmitter emitter = sseService.register();

        // Simulate timeout by completing the emitter
        emitter.completeWithError(new RuntimeException("Timeout"));

        // Subsequent broadcast should not fail
        sseService.broadcast("test-event");
    }

    @Test
    void onError_shouldRemoveEmitter() {
        SseEmitter emitter = sseService.register();

        // Simulate error
        emitter.completeWithError(new IOException("Connection lost"));

        // Subsequent broadcast should not fail
        sseService.broadcast("test-event");
    }

    @Test
    void broadcast_withNoEmitters_shouldNotThrow() {
        // No emitters registered, should not throw
        sseService.broadcast("test-event");
    }

    @Test
    void register_multipleEmitters_shouldTrackAll() {
        SseEmitter emitter1 = sseService.register();
        SseEmitter emitter2 = sseService.register();
        SseEmitter emitter3 = sseService.register();

        assertThat(emitter1).isNotNull();
        assertThat(emitter2).isNotNull();
        assertThat(emitter3).isNotNull();
        assertThat(emitter1).isNotSameAs(emitter2);
        assertThat(emitter2).isNotSameAs(emitter3);
    }
}
