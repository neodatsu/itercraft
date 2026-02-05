package com.itercraft.api.application.claude;

public record GameInfoResponse(
    String nom,
    String description,
    String typeCode,
    short joueursMin,
    short joueursMax,
    String ageCode,
    Short dureeMoyenneMinutes,
    short complexiteNiveau,
    String imageUrl
) {}
