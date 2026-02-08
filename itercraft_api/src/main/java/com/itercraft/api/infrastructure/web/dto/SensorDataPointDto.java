package com.itercraft.api.infrastructure.web.dto;

import java.time.OffsetDateTime;

public record SensorDataPointDto(
    OffsetDateTime measuredAt,
    String deviceName,
    Double dhtTemperature,
    Double dhtHumidity,
    Double ntcTemperature,
    Double luminosity
) {}
