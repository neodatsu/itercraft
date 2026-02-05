package com.itercraft.api.domain.ludotheque;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ComplexiteJeuRepository extends JpaRepository<ComplexiteJeu, UUID> {
    Optional<ComplexiteJeu> findByNiveau(Short niveau);
}
