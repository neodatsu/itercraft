package com.itercraft.api.domain.ludotheque;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JeuRepository extends JpaRepository<Jeu, UUID> {
    Optional<Jeu> findByNomIgnoreCase(String nom);
}
