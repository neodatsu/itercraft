package com.itercraft.api.application.ludotheque;

import java.util.UUID;

public record JeuDto(
    UUID id,
    String nom,
    String description,
    String typeCode,
    String typeLibelle,
    short joueursMin,
    short joueursMax,
    String ageCode,
    String ageLibelle,
    Short dureeMoyenneMinutes,
    short complexiteNiveau,
    String complexiteLibelle,
    String imageUrl
) {}
