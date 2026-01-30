package com.itercraft.api.domain.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "service", schema = "itercraft")
public class ServiceEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false)
    private String label;

    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ServiceEntity() {}

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
