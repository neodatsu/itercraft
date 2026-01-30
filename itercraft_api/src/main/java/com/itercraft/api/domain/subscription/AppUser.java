package com.itercraft.api.domain.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_user", schema = "itercraft")
public class AppUser {

    @Id
    private UUID id;

    @Column(name = "keycloak_sub", nullable = false, unique = true)
    private String keycloakSub;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AppUser() {}

    public AppUser(String keycloakSub) {
        this.id = UUID.randomUUID();
        this.keycloakSub = keycloakSub;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getKeycloakSub() { return keycloakSub; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
