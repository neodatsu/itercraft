package com.itercraft.api.infrastructure.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record MqttSensorPayload(
    @JsonProperty("timestamp") OffsetDateTime timestamp,
    @JsonProperty("user") String user,
    @JsonProperty("device") String device,
    @JsonProperty("dht_temperature") Double dhtTemperature,
    @JsonProperty("dht_humidity") Double dhtHumidity,
    @JsonProperty("ntc_temperature") Double ntcTemperature,
    @JsonProperty("luminosity") Double luminosity
) {}
