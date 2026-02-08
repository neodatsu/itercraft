package com.itercraft.api.application.sensor;

import com.itercraft.api.domain.sensor.SensorData;
import com.itercraft.api.domain.sensor.SensorDataRepository;
import com.itercraft.api.domain.sensor.SensorDevice;
import com.itercraft.api.domain.sensor.SensorDeviceRepository;
import com.itercraft.api.domain.subscription.AppUser;
import com.itercraft.api.domain.subscription.AppUserRepository;
import com.itercraft.api.infrastructure.sse.SseService;
import com.itercraft.api.infrastructure.web.dto.SensorDataPointDto;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class SensorDataServiceImpl implements SensorDataService {

    private final AppUserRepository appUserRepository;
    private final SensorDeviceRepository sensorDeviceRepository;
    private final SensorDataRepository sensorDataRepository;
    private final SseService sseService;
    private static final String SSE_EVENT_TYPE = "sensor-data-change";

    public SensorDataServiceImpl(AppUserRepository appUserRepository,
                                  SensorDeviceRepository sensorDeviceRepository,
                                  SensorDataRepository sensorDataRepository,
                                  SseService sseService) {
        this.appUserRepository = appUserRepository;
        this.sensorDeviceRepository = sensorDeviceRepository;
        this.sensorDataRepository = sensorDataRepository;
        this.sseService = sseService;
    }

    @Override
    public void ingestSensorData(String userEmail, String deviceName,
                                  OffsetDateTime measuredAt,
                                  Double dhtTemperature, Double dhtHumidity,
                                  Double ntcTemperature, Double luminosity) {
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé: " + userEmail));

        SensorDevice device = sensorDeviceRepository.findByUserAndName(user, deviceName)
                .orElseGet(() -> sensorDeviceRepository.save(SensorDevice.create(user, deviceName)));

        SensorData data = SensorData.create(device, measuredAt,
                dhtTemperature, dhtHumidity, ntcTemperature, luminosity);
        sensorDataRepository.save(data);

        sseService.broadcast(SSE_EVENT_TYPE);
    }

    @Override
    public List<SensorDataPointDto> getSensorData(String keycloakSub,
                                                    OffsetDateTime from,
                                                    OffsetDateTime to) {
        AppUser user = appUserRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé"));

        return sensorDataRepository.findByUserAndDateRange(user, from, to)
                .stream()
                .map(sd -> new SensorDataPointDto(
                        sd.getMeasuredAt(),
                        sd.getDevice().getName(),
                        sd.getDhtTemperature(),
                        sd.getDhtHumidity(),
                        sd.getNtcTemperature(),
                        sd.getLuminosity()
                ))
                .toList();
    }
}
