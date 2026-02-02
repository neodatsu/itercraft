package com.itercraft.api.application.meteo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeteoServiceImplTest {

    @Test
    void constructor_shouldCreateServiceWithoutError() {
        MeteoServiceImpl service = new MeteoServiceImpl(
                "https://public-api.meteofrance.fr/public/aromepi/1.0",
                "test-token");
        assertThat(service).isNotNull();
    }
}
