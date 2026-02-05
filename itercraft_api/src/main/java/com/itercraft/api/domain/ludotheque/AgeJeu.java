package com.itercraft.api.domain.ludotheque;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "age_jeu", schema = "itercraft")
public class AgeJeu {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 50)
    private String libelle;

    @Column(name = "age_minimum")
    private Short ageMinimum;

    protected AgeJeu() {}

    public AgeJeu(UUID id, String code, String libelle, Short ageMinimum) {
        this.id = id;
        this.code = code;
        this.libelle = libelle;
        this.ageMinimum = ageMinimum;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getLibelle() { return libelle; }
    public Short getAgeMinimum() { return ageMinimum; }
}
