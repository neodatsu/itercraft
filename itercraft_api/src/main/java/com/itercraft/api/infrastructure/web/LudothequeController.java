package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.ludotheque.AddJeuRequest;
import com.itercraft.api.application.ludotheque.GameSuggestionDto;
import com.itercraft.api.application.ludotheque.JeuDto;
import com.itercraft.api.application.ludotheque.JeuUserDto;
import com.itercraft.api.application.ludotheque.LudothequeService;
import com.itercraft.api.application.ludotheque.ReferenceTablesDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ludotheque")
public class LudothequeController {

    private final LudothequeService ludothequeService;

    public LudothequeController(LudothequeService ludothequeService) {
        this.ludothequeService = ludothequeService;
    }

    /**
     * Récupère tous les jeux du catalogue.
     */
    @GetMapping("/jeux")
    public ResponseEntity<List<JeuDto>> getAllJeux() {
        return ResponseEntity.ok(ludothequeService.getAllJeux());
    }

    /**
     * Récupère la ludothèque de l'utilisateur connecté.
     */
    @GetMapping("/mes-jeux")
    public ResponseEntity<List<JeuUserDto>> getMesJeux(JwtAuthenticationToken auth) {
        String userSub = getUserSub(auth);
        String userEmail = getUserEmail(auth);
        return ResponseEntity.ok(ludothequeService.getUserLudotheque(userSub, userEmail));
    }

    /**
     * Ajoute un jeu à la ludothèque de l'utilisateur.
     */
    @PostMapping("/mes-jeux/{jeuId}")
    public ResponseEntity<Void> addJeu(
            @PathVariable UUID jeuId,
            JwtAuthenticationToken auth) {
        String userSub = getUserSub(auth);
        ludothequeService.addJeuToUser(userSub, jeuId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Retire un jeu de la ludothèque de l'utilisateur.
     */
    @DeleteMapping("/mes-jeux/{jeuId}")
    public ResponseEntity<Void> removeJeu(
            @PathVariable UUID jeuId,
            JwtAuthenticationToken auth) {
        String userSub = getUserSub(auth);
        ludothequeService.removeJeuFromUser(userSub, jeuId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Met à jour la note d'un jeu.
     */
    @PutMapping("/mes-jeux/{jeuId}/note")
    public ResponseEntity<Void> updateNote(
            @PathVariable UUID jeuId,
            @RequestBody Map<String, Short> body,
            JwtAuthenticationToken auth) {
        String userSub = getUserSub(auth);
        Short note = body.get("note");
        ludothequeService.updateNote(userSub, jeuId, note);
        return ResponseEntity.ok().build();
    }

    /**
     * Initialise la ludothèque avec tous les jeux du catalogue.
     */
    @PostMapping("/mes-jeux/initialize-all")
    public ResponseEntity<Map<String, Object>> initializeAll(JwtAuthenticationToken auth) {
        String userSub = getUserSub(auth);
        int added = ludothequeService.initializeAllGames(userSub);
        return ResponseEntity.ok(Map.of(
                "message", "Ludothèque initialisée",
                "jeuxAjoutes", added
        ));
    }

    /**
     * Ajoute un jeu par son titre (Claude remplit les détails).
     */
    @PostMapping("/mes-jeux")
    public ResponseEntity<JeuUserDto> addJeuByTitle(
            @RequestBody AddJeuRequest request,
            JwtAuthenticationToken auth) {
        String userSub = getUserSub(auth);
        if (request.nom() == null || request.nom().isBlank()) {
            throw new IllegalArgumentException("Le nom du jeu est requis");
        }
        JeuUserDto result = ludothequeService.addJeuByTitle(userSub, request.nom());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Suggère un nouveau jeu basé sur les notes de l'utilisateur.
     */
    @PostMapping("/suggestion")
    public ResponseEntity<GameSuggestionDto> getSuggestion(JwtAuthenticationToken auth) {
        String userSub = getUserSub(auth);
        return ResponseEntity.ok(ludothequeService.getSuggestion(userSub));
    }

    /**
     * Récupère les tables de référence pour les filtres.
     */
    @GetMapping("/references")
    public ResponseEntity<ReferenceTablesDto> getReferences() {
        return ResponseEntity.ok(ludothequeService.getReferenceTables());
    }

    private String getUserSub(JwtAuthenticationToken auth) {
        String sub = auth.getToken().getSubject();
        if (sub == null) {
            throw new IllegalStateException("JWT does not contain 'sub' claim");
        }
        return sub;
    }

    private String getUserEmail(JwtAuthenticationToken auth) {
        Object email = auth.getToken().getClaim("email");
        return email != null ? email.toString() : null;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
