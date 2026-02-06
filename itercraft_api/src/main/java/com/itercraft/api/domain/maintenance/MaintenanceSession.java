package com.itercraft.api.domain.maintenance;

import com.itercraft.api.domain.subscription.AppUser;
import com.itercraft.api.domain.subscription.ServiceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "maintenance_session", schema = "itercraft")
public class MaintenanceSession {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "auto_stopped")
    private Boolean autoStopped;

    protected MaintenanceSession() {}

    public static MaintenanceSession start(AppUser user, ServiceEntity service) {
        MaintenanceSession session = new MaintenanceSession();
        session.id = UUID.randomUUID();
        session.user = user;
        session.service = service;
        session.startedAt = OffsetDateTime.now();
        session.autoStopped = false;
        return session;
    }

    public void stop(boolean autoStopped) {
        if (this.endedAt != null) {
            throw new IllegalStateException("Session already ended");
        }
        this.endedAt = OffsetDateTime.now();
        this.autoStopped = autoStopped;
        this.durationMinutes = (int) Duration.between(startedAt, endedAt).toMinutes();
    }

    public boolean isActive() {
        return endedAt == null;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public ServiceEntity getService() { return service; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getEndedAt() { return endedAt; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public Boolean getAutoStopped() { return autoStopped; }
}
