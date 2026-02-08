package com.itercraft.api.domain.sensor;

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
@Table(name = "sensor_data", schema = "itercraft")
public class SensorData {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private SensorDevice device;

    @Column(name = "measured_at", nullable = false)
    private OffsetDateTime measuredAt;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "dht_temperature")
    private Double dhtTemperature;

    @Column(name = "dht_humidity")
    private Double dhtHumidity;

    @Column(name = "ntc_temperature")
    private Double ntcTemperature;

    @Column(name = "luminosity")
    private Double luminosity;

    protected SensorData() {}

    public static SensorData create(SensorDevice device, OffsetDateTime measuredAt,
                                     Double dhtTemperature, Double dhtHumidity,
                                     Double ntcTemperature, Double luminosity) {
        SensorData data = new SensorData();
        data.id = UUID.randomUUID();
        data.device = device;
        data.measuredAt = measuredAt;
        data.receivedAt = OffsetDateTime.now();
        data.dhtTemperature = dhtTemperature;
        data.dhtHumidity = dhtHumidity;
        data.ntcTemperature = ntcTemperature;
        data.luminosity = luminosity;
        return data;
    }

    public UUID getId() { return id; }
    public SensorDevice getDevice() { return device; }
    public OffsetDateTime getMeasuredAt() { return measuredAt; }
    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public Double getDhtTemperature() { return dhtTemperature; }
    public Double getDhtHumidity() { return dhtHumidity; }
    public Double getNtcTemperature() { return ntcTemperature; }
    public Double getLuminosity() { return luminosity; }
}
