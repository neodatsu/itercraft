package com.itercraft.api.domain.ludotheque;

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
@Table(name = "jeu_user", schema = "itercraft")
public class JeuUser {

    @Id
    private UUID id;

    @Column(name = "user_sub", nullable = false, length = 100)
    private String userSub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jeu_id", nullable = false)
    private Jeu jeu;

    private Short note;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected JeuUser() {}

    public JeuUser(String userSub, Jeu jeu) {
        this.id = UUID.randomUUID();
        this.userSub = userSub;
        this.jeu = jeu;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getUserSub() { return userSub; }
    public Jeu getJeu() { return jeu; }
    public Short getNote() { return note; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setNote(Short note) {
        if (note != null && (note < 1 || note > 5)) {
            throw new IllegalArgumentException("La note doit être entre 1 et 5");
        }
        this.note = note;
    }
}
