package com.itercraft.api.domain.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscription", schema = "itercraft",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "service_id"}))
public class Subscription {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(name = "subscribed_at", nullable = false)
    private OffsetDateTime subscribedAt;

    protected Subscription() {}

    public Subscription(AppUser user, ServiceEntity service) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.service = service;
        this.subscribedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public ServiceEntity getService() { return service; }
    public OffsetDateTime getSubscribedAt() { return subscribedAt; }
}
