package com.itercraft.api.domain.subscription;

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
@Table(name = "service_usage", schema = "itercraft")
public class ServiceUsage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "used_at", nullable = false)
    private OffsetDateTime usedAt;

    protected ServiceUsage() {}

    public ServiceUsage(Subscription subscription) {
        this.id = UUID.randomUUID();
        this.subscription = subscription;
        this.usedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public Subscription getSubscription() { return subscription; }
    public OffsetDateTime getUsedAt() { return usedAt; }
}
