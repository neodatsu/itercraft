package com.itercraft.api.domain.sensor;

import com.itercraft.api.domain.subscription.AppUser;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensorDataTest {

    @Test
    void create_shouldInitializeAllFields() {
        AppUser user = new AppUser("sub-1");
        SensorDevice device = SensorDevice.create(user, "station-1");
        OffsetDateTime measuredAt = OffsetDateTime.parse("2026-02-08T12:00:00+01:00");

        SensorData data = SensorData.create(device, measuredAt, 22.5, 48.0, 23.1, 55.0);

        assertThat(data.getId()).isNotNull();
        assertThat(data.getDevice()).isEqualTo(device);
        assertThat(data.getMeasuredAt()).isEqualTo(measuredAt);
        assertThat(data.getReceivedAt()).isNotNull();
        assertThat(data.getDhtTemperature()).isEqualTo(22.5);
        assertThat(data.getDhtHumidity()).isEqualTo(48.0);
        assertThat(data.getNtcTemperature()).isEqualTo(23.1);
        assertThat(data.getLuminosity()).isEqualTo(55.0);
    }

    @Test
    void create_shouldGenerateUniqueIds() {
        AppUser user = new AppUser("sub-1");
        SensorDevice device = SensorDevice.create(user, "station-1");
        OffsetDateTime measuredAt = OffsetDateTime.now();

        SensorData data1 = SensorData.create(device, measuredAt, 20.0, 50.0, 21.0, 60.0);
        SensorData data2 = SensorData.create(device, measuredAt, 20.0, 50.0, 21.0, 60.0);

        assertThat(data1.getId()).isNotEqualTo(data2.getId());
    }

    @Test
    void create_shouldAcceptNullValues() {
        AppUser user = new AppUser("sub-1");
        SensorDevice device = SensorDevice.create(user, "station-1");

        SensorData data = SensorData.create(device, OffsetDateTime.now(), null, null, null, null);

        assertThat(data.getDhtTemperature()).isNull();
        assertThat(data.getDhtHumidity()).isNull();
        assertThat(data.getNtcTemperature()).isNull();
        assertThat(data.getLuminosity()).isNull();
    }
}
