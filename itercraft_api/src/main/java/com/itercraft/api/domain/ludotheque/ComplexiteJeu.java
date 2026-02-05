package com.itercraft.api.domain.ludotheque;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "complexite_jeu", schema = "itercraft")
public class ComplexiteJeu {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private Short niveau;

    @Column(nullable = false, length = 100)
    private String libelle;

    protected ComplexiteJeu() {}

    public ComplexiteJeu(UUID id, Short niveau, String libelle) {
        this.id = id;
        this.niveau = niveau;
        this.libelle = libelle;
    }

    public UUID getId() { return id; }
    public Short getNiveau() { return niveau; }
    public String getLibelle() { return libelle; }
}
