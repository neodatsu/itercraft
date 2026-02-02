package com.itercraft.api.application.ollama;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaServiceImplTest {

    @Test
    void constructor_shouldCreateServiceWithoutError() {
        OllamaServiceImpl service = new OllamaServiceImpl(
                "http://localhost:11434", "llava");
        assertThat(service).isNotNull();
    }
}
