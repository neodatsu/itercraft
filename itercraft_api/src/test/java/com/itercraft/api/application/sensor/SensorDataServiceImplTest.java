package com.itercraft.api.application.sensor;

import com.itercraft.api.domain.sensor.SensorData;
import com.itercraft.api.domain.sensor.SensorDataRepository;
import com.itercraft.api.domain.sensor.SensorDevice;
import com.itercraft.api.domain.sensor.SensorDeviceRepository;
import com.itercraft.api.domain.subscription.AppUser;
import com.itercraft.api.domain.subscription.AppUserRepository;
import com.itercraft.api.infrastructure.sse.SseService;
import com.itercraft.api.infrastructure.web.dto.SensorDataPointDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorDataServiceImplTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private SensorDeviceRepository sensorDeviceRepository;
    @Mock private SensorDataRepository sensorDataRepository;
    @Mock private SseService sseService;

    @InjectMocks
    private SensorDataServiceImpl sensorDataService;

    private static final String SUB = "user-sub-123";
    private static final String EMAIL = "laurent@itercraft.com";
    private static final String DEVICE_NAME = "meteoStation_1";

    @Test
    void ingestSensorData_shouldCreateDeviceAndSaveData() {
        AppUser user = new AppUser(SUB);
        user.setEmail(EMAIL);
        SensorDevice device = SensorDevice.create(user, DEVICE_NAME);

        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(sensorDeviceRepository.findByUserAndName(user, DEVICE_NAME)).thenReturn(Optional.empty());
        when(sensorDeviceRepository.save(any(SensorDevice.class))).thenReturn(device);
        when(sensorDataRepository.save(any(SensorData.class))).thenAnswer(i -> i.getArgument(0));

        OffsetDateTime now = OffsetDateTime.now();
        sensorDataService.ingestSensorData(EMAIL, DEVICE_NAME, now, 20.7, 52.0, 21.1, 77.0);

        verify(sensorDeviceRepository).save(any(SensorDevice.class));
        verify(sensorDataRepository).save(any(SensorData.class));
        verify(sseService).broadcast("sensor-data-change");
    }

    @Test
    void ingestSensorData_shouldReuseExistingDevice() {
        AppUser user = new AppUser(SUB);
        user.setEmail(EMAIL);
        SensorDevice device = SensorDevice.create(user, DEVICE_NAME);

        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(sensorDeviceRepository.findByUserAndName(user, DEVICE_NAME)).thenReturn(Optional.of(device));
        when(sensorDataRepository.save(any(SensorData.class))).thenAnswer(i -> i.getArgument(0));

        OffsetDateTime now = OffsetDateTime.now();
        sensorDataService.ingestSensorData(EMAIL, DEVICE_NAME, now, 20.7, 52.0, 21.1, 77.0);

        verify(sensorDataRepository).save(any(SensorData.class));
        verify(sseService).broadcast("sensor-data-change");
    }

    @Test
    void ingestSensorData_shouldThrowWhenUserNotFound() {
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sensorDataService.ingestSensorData(
                EMAIL, DEVICE_NAME, OffsetDateTime.now(), 20.7, 52.0, 21.1, 77.0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Utilisateur non trouvé");
    }

    @Test
    void ingestSensorData_shouldHandleNullDhtValues() {
        AppUser user = new AppUser(SUB);
        user.setEmail(EMAIL);
        SensorDevice device = SensorDevice.create(user, DEVICE_NAME);

        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(sensorDeviceRepository.findByUserAndName(user, DEVICE_NAME)).thenReturn(Optional.of(device));
        when(sensorDataRepository.save(any(SensorData.class))).thenAnswer(i -> i.getArgument(0));

        OffsetDateTime now = OffsetDateTime.now();
        sensorDataService.ingestSensorData(EMAIL, DEVICE_NAME, now, null, null, 21.1, 77.0);

        verify(sensorDataRepository).save(any(SensorData.class));
    }

    @Test
    void getSensorData_shouldReturnMappedDtos() {
        AppUser user = new AppUser(SUB);
        SensorDevice device = SensorDevice.create(user, DEVICE_NAME);
        OffsetDateTime now = OffsetDateTime.now();
        SensorData data = SensorData.create(device, now, 20.7, 52.0, 21.1, 77.0);

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(sensorDataRepository.findByUserAndDateRange(any(), any(), any())).thenReturn(List.of(data));

        List<SensorDataPointDto> result = sensorDataService.getSensorData(SUB,
                now.minusDays(7), now);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).deviceName()).isEqualTo(DEVICE_NAME);
        assertThat(result.get(0).dhtTemperature()).isEqualTo(20.7);
        assertThat(result.get(0).dhtHumidity()).isEqualTo(52.0);
        assertThat(result.get(0).ntcTemperature()).isEqualTo(21.1);
        assertThat(result.get(0).luminosity()).isEqualTo(77.0);
    }

    @Test
    void getSensorData_shouldThrowWhenUserNotFound() {
        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sensorDataService.getSensorData(SUB,
                OffsetDateTime.now().minusDays(7), OffsetDateTime.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Utilisateur non trouvé");
    }
}
