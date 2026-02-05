package com.itercraft.api.domain.ludotheque;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TypeJeuRepository extends JpaRepository<TypeJeu, UUID> {
    Optional<TypeJeu> findByCode(String code);
}
