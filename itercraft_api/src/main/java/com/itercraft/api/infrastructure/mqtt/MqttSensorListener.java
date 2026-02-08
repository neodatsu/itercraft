package com.itercraft.api.infrastructure.mqtt;

import tools.jackson.databind.ObjectMapper;
import com.itercraft.api.application.sensor.SensorDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MqttSensorListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttSensorListener.class);

    private final SensorDataService sensorDataService;
    private final ObjectMapper objectMapper;

    public MqttSensorListener(SensorDataService sensorDataService, ObjectMapper objectMapper) {
        this.sensorDataService = sensorDataService;
        this.objectMapper = objectMapper;
    }

    public void handleMessage(String payload) {
        try {
            MqttSensorPayload data = objectMapper.readValue(payload, MqttSensorPayload.class);
            sensorDataService.ingestSensorData(
                    data.user(),
                    data.device(),
                    data.timestamp(),
                    data.dhtTemperature(),
                    data.dhtHumidity(),
                    data.ntcTemperature(),
                    data.luminosity()
            );
            LOGGER.info("Sensor data ingested for {}/{}", data.user(), data.device());
        } catch (Exception e) {
            LOGGER.warn("Failed to process MQTT sensor message", e);
        }
    }
}
