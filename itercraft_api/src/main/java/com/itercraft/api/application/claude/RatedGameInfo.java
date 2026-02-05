package com.itercraft.api.application.claude;

public record RatedGameInfo(
    String nom,
    String typeCode,
    short note,
    String description
) {}
