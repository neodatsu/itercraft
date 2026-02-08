package com.itercraft.api.domain.sensor;

import com.itercraft.api.domain.subscription.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sensor_device", schema = "itercraft")
public class SensorDevice {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SensorDevice() {}

    public static SensorDevice create(AppUser user, String name) {
        SensorDevice device = new SensorDevice();
        device.id = UUID.randomUUID();
        device.user = user;
        device.name = name;
        device.createdAt = OffsetDateTime.now();
        return device;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getName() { return name; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
