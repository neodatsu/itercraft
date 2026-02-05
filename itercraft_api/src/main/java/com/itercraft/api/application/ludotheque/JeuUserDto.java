package com.itercraft.api.application.ludotheque;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JeuUserDto(
    UUID id,
    JeuDto jeu,
    Short note,
    OffsetDateTime addedAt
) {}
