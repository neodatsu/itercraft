package com.itercraft.api.application.ludotheque;

import java.util.List;
import java.util.UUID;

public record ReferenceTablesDto(
    List<TypeJeuRef> types,
    List<AgeJeuRef> ages,
    List<ComplexiteJeuRef> complexites
) {
    public record TypeJeuRef(UUID id, String code, String libelle) {}
    public record AgeJeuRef(UUID id, String code, String libelle, Short ageMinimum) {}
    public record ComplexiteJeuRef(UUID id, Short niveau, String libelle) {}
}
