package com.itercraft.api.application.claude;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClaudeServiceImplTest {

    @Test
    void constructor_shouldCreateServiceWithoutError() {
        ClaudeServiceImpl service = new ClaudeServiceImpl(
                "test-key", "claude-sonnet-4-20250514");
        assertThat(service).isNotNull();
    }
}
