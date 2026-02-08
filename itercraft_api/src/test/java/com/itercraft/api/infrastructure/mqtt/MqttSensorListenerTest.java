package com.itercraft.api.infrastructure.mqtt;

import tools.jackson.databind.ObjectMapper;
import com.itercraft.api.application.sensor.SensorDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MqttSensorListenerTest {

    @Mock
    private SensorDataService sensorDataService;

    private MqttSensorListener mqttSensorListener;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        mqttSensorListener = new MqttSensorListener(sensorDataService, objectMapper);
    }

    @Test
    void handleMessage_shouldParseAndIngest() {
        String json = """
                {
                    "timestamp": "2026-02-08T15:30:00+01:00",
                    "user": "laurent@itercraft.com",
                    "device": "meteoStation_1",
                    "dht_temperature": 20.7,
                    "dht_humidity": 52.0,
                    "ntc_temperature": 21.1,
                    "luminosity": 77.0
                }
                """;

        mqttSensorListener.handleMessage(json);

        verify(sensorDataService).ingestSensorData(
                eq("laurent@itercraft.com"),
                eq("meteoStation_1"),
                any(OffsetDateTime.class),
                eq(20.7),
                eq(52.0),
                eq(21.1),
                eq(77.0)
        );
    }

    @Test
    void handleMessage_shouldHandleNullDhtValues() {
        String json = """
                {
                    "timestamp": "2026-02-08T15:30:00+01:00",
                    "user": "laurent@itercraft.com",
                    "device": "meteoStation_1",
                    "dht_temperature": null,
                    "dht_humidity": null,
                    "ntc_temperature": 21.1,
                    "luminosity": 77.0
                }
                """;

        mqttSensorListener.handleMessage(json);

        verify(sensorDataService).ingestSensorData(
                eq("laurent@itercraft.com"),
                eq("meteoStation_1"),
                any(OffsetDateTime.class),
                eq(null),
                eq(null),
                eq(21.1),
                eq(77.0)
        );
    }

    @Test
    void handleMessage_shouldNotCrashOnMalformedJson() {
        mqttSensorListener.handleMessage("not json at all");

        verify(sensorDataService, never()).ingestSensorData(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void handleMessage_shouldNotCrashOnEmptyPayload() {
        mqttSensorListener.handleMessage("");

        verify(sensorDataService, never()).ingestSensorData(any(), any(), any(), any(), any(), any(), any());
    }
}
