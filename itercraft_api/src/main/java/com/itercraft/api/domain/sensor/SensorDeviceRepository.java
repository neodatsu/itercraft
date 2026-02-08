package com.itercraft.api.domain.sensor;

import com.itercraft.api.domain.subscription.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorDeviceRepository extends JpaRepository<SensorDevice, UUID> {
    Optional<SensorDevice> findByUserAndName(AppUser user, String name);
}
