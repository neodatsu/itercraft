package com.itercraft.api.domain.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "activity_analysis", schema = "itercraft")
public class ActivityAnalysis {

    @Id
    private UUID id;

    @Column(name = "location_name", nullable = false, length = 200)
    private String locationName;

    @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "analysis_date", nullable = false)
    private LocalDate analysisDate;

    @Column(name = "response_json", nullable = false, columnDefinition = "text")
    private String responseJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ActivityAnalysis() {}

    public ActivityAnalysis(String locationName, double latitude, double longitude, String responseJson) {
        this.id = UUID.randomUUID();
        this.locationName = locationName;
        this.latitude = BigDecimal.valueOf(latitude);
        this.longitude = BigDecimal.valueOf(longitude);
        this.analysisDate = LocalDate.now();
        this.responseJson = responseJson;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getLocationName() { return locationName; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public LocalDate getAnalysisDate() { return analysisDate; }
    public String getResponseJson() { return responseJson; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
