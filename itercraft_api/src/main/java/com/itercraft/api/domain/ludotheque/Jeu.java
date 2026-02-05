package com.itercraft.api.domain.ludotheque;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "jeu", schema = "itercraft")
public class Jeu {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_jeu_id", nullable = false)
    private TypeJeu typeJeu;

    @Column(name = "joueurs_min", nullable = false)
    private Short joueursMin;

    @Column(name = "joueurs_max", nullable = false)
    private Short joueursMax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "age_jeu_id", nullable = false)
    private AgeJeu ageJeu;

    @Column(name = "duree_moyenne_minutes")
    private Short dureeMoyenneMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complexite_id", nullable = false)
    private ComplexiteJeu complexite;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    protected Jeu() {}

    private Jeu(Builder builder) {
        this.id = UUID.randomUUID();
        this.nom = builder.nom;
        this.description = builder.description;
        this.typeJeu = builder.typeJeu;
        this.joueursMin = builder.joueursMin;
        this.joueursMax = builder.joueursMax;
        this.ageJeu = builder.ageJeu;
        this.dureeMoyenneMinutes = builder.dureeMoyenneMinutes;
        this.complexite = builder.complexite;
        this.imageUrl = builder.imageUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public String getNom() { return nom; }
    public String getDescription() { return description; }
    public TypeJeu getTypeJeu() { return typeJeu; }
    public Short getJoueursMin() { return joueursMin; }
    public Short getJoueursMax() { return joueursMax; }
    public AgeJeu getAgeJeu() { return ageJeu; }
    public Short getDureeMoyenneMinutes() { return dureeMoyenneMinutes; }
    public ComplexiteJeu getComplexite() { return complexite; }
    public String getImageUrl() { return imageUrl; }

    public static class Builder {
        private String nom;
        private String description;
        private TypeJeu typeJeu;
        private Short joueursMin;
        private Short joueursMax;
        private AgeJeu ageJeu;
        private Short dureeMoyenneMinutes;
        private ComplexiteJeu complexite;
        private String imageUrl;

        public Builder nom(String nom) {
            this.nom = nom;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder typeJeu(TypeJeu typeJeu) {
            this.typeJeu = typeJeu;
            return this;
        }

        public Builder joueursMin(short joueursMin) {
            this.joueursMin = joueursMin;
            return this;
        }

        public Builder joueursMax(short joueursMax) {
            this.joueursMax = joueursMax;
            return this;
        }

        public Builder ageJeu(AgeJeu ageJeu) {
            this.ageJeu = ageJeu;
            return this;
        }

        public Builder dureeMoyenneMinutes(Short dureeMoyenneMinutes) {
            this.dureeMoyenneMinutes = dureeMoyenneMinutes;
            return this;
        }

        public Builder complexite(ComplexiteJeu complexite) {
            this.complexite = complexite;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Jeu build() {
            return new Jeu(this);
        }
    }
}
