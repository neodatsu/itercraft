package com.itercraft.api.application.sensor;

import com.itercraft.api.infrastructure.web.dto.SensorDataPointDto;
import java.time.OffsetDateTime;
import java.util.List;

public interface SensorDataService {

    void ingestSensorData(String userEmail, String deviceName,
                          OffsetDateTime measuredAt,
                          Double dhtTemperature, Double dhtHumidity,
                          Double ntcTemperature, Double luminosity);

    List<SensorDataPointDto> getSensorData(String keycloakSub,
                                            OffsetDateTime from,
                                            OffsetDateTime to);
}
