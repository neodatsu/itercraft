package com.itercraft.api.application.ludotheque;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request DTO for updating a game rating.
 * Note must be between 1 and 5 (inclusive), or null to clear the rating.
 */
public record UpdateNoteRequest(
        @Min(value = 1, message = "La note doit être comprise entre 1 et 5")
        @Max(value = 5, message = "La note doit être comprise entre 1 et 5")
        Short note
) {}
