package com.itercraft.api.infrastructure.mqtt;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class MqttSensorPayloadTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeFromSnakeCaseJson() throws Exception {
        String json = """
                {
                    "timestamp": "2026-02-08T17:00:00+01:00",
                    "user": "user@example.com",
                    "device": "station-1",
                    "dht_temperature": 22.5,
                    "dht_humidity": 49.0,
                    "ntc_temperature": 23.1,
                    "luminosity": 55.0
                }
                """;

        MqttSensorPayload payload = objectMapper.readValue(json, MqttSensorPayload.class);

        assertThat(payload.timestamp()).isEqualTo(OffsetDateTime.parse("2026-02-08T17:00:00+01:00"));
        assertThat(payload.user()).isEqualTo("user@example.com");
        assertThat(payload.device()).isEqualTo("station-1");
        assertThat(payload.dhtTemperature()).isEqualTo(22.5);
        assertThat(payload.dhtHumidity()).isEqualTo(49.0);
        assertThat(payload.ntcTemperature()).isEqualTo(23.1);
        assertThat(payload.luminosity()).isEqualTo(55.0);
    }

    @Test
    void shouldDeserializeWithNullValues() throws Exception {
        String json = """
                {
                    "timestamp": "2026-02-08T17:00:00+01:00",
                    "user": "user@example.com",
                    "device": "station-1",
                    "dht_temperature": null,
                    "dht_humidity": null,
                    "ntc_temperature": 23.1,
                    "luminosity": null
                }
                """;

        MqttSensorPayload payload = objectMapper.readValue(json, MqttSensorPayload.class);

        assertThat(payload.dhtTemperature()).isNull();
        assertThat(payload.dhtHumidity()).isNull();
        assertThat(payload.ntcTemperature()).isEqualTo(23.1);
        assertThat(payload.luminosity()).isNull();
    }
}
