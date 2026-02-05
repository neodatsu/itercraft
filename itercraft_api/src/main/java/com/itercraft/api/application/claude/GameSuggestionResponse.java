package com.itercraft.api.application.claude;

public record GameSuggestionResponse(
    String nom,
    String description,
    String typeCode,
    String raison,
    String imageUrl
) {}
