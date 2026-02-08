package com.itercraft.api.domain.sensor;

import com.itercraft.api.domain.subscription.AppUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensorDeviceTest {

    @Test
    void create_shouldInitializeAllFields() {
        AppUser user = new AppUser("sub-1");

        SensorDevice device = SensorDevice.create(user, "meteoStation_1");

        assertThat(device.getId()).isNotNull();
        assertThat(device.getUser()).isEqualTo(user);
        assertThat(device.getName()).isEqualTo("meteoStation_1");
        assertThat(device.getCreatedAt()).isNotNull();
    }

    @Test
    void create_shouldGenerateUniqueIds() {
        AppUser user = new AppUser("sub-1");

        SensorDevice device1 = SensorDevice.create(user, "station-1");
        SensorDevice device2 = SensorDevice.create(user, "station-2");

        assertThat(device1.getId()).isNotEqualTo(device2.getId());
    }
}
