package com.itercraft.api.domain.ludotheque;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgeJeuRepository extends JpaRepository<AgeJeu, UUID> {
    Optional<AgeJeu> findByCode(String code);
}
