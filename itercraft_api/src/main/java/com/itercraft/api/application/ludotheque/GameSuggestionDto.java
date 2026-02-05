package com.itercraft.api.application.ludotheque;

public record GameSuggestionDto(
    String nom,
    String description,
    String typeCode,
    String typeLibelle,
    String raison,
    String imageUrl
) {}
