package com.itercraft.api.application.ludotheque;

import java.util.List;
import java.util.UUID;

public interface LudothequeService {

    /**
     * Récupère la ludothèque d'un utilisateur.
     * Si l'email correspond à INIT_USER_EMAIL et la collection est vide, elle est auto-initialisée.
     */
    List<JeuUserDto> getUserLudotheque(String userSub, String userEmail);

    /**
     * Récupère tous les jeux disponibles dans le catalogue.
     */
    List<JeuDto> getAllJeux();

    /**
     * Ajoute un jeu à la ludothèque de l'utilisateur.
     */
    void addJeuToUser(String userSub, UUID jeuId);

    /**
     * Retire un jeu de la ludothèque de l'utilisateur.
     */
    void removeJeuFromUser(String userSub, UUID jeuId);

    /**
     * Met à jour la note d'un jeu pour un utilisateur.
     */
    void updateNote(String userSub, UUID jeuId, Short note);

    /**
     * Initialise la ludothèque avec tous les jeux du catalogue.
     */
    int initializeAllGames(String userSub);

    /**
     * Ajoute un jeu par son titre. Claude remplit automatiquement les détails.
     */
    JeuUserDto addJeuByTitle(String userSub, String nom);

    /**
     * Suggère un nouveau jeu basé sur les notes de l'utilisateur.
     */
    GameSuggestionDto getSuggestion(String userSub);

    /**
     * Récupère les tables de référence pour les filtres.
     */
    ReferenceTablesDto getReferenceTables();
}
