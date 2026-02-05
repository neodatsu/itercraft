package com.itercraft.api.application.ludotheque;

import com.itercraft.api.application.claude.ClaudeService;
import com.itercraft.api.application.claude.GameInfoResponse;
import com.itercraft.api.application.claude.GameSuggestionResponse;
import com.itercraft.api.application.claude.RatedGameInfo;
import com.itercraft.api.domain.ludotheque.AgeJeu;
import com.itercraft.api.domain.ludotheque.AgeJeuRepository;
import com.itercraft.api.domain.ludotheque.ComplexiteJeu;
import com.itercraft.api.domain.ludotheque.ComplexiteJeuRepository;
import com.itercraft.api.domain.ludotheque.Jeu;
import com.itercraft.api.domain.ludotheque.JeuRepository;
import com.itercraft.api.domain.ludotheque.JeuUser;
import com.itercraft.api.domain.ludotheque.JeuUserRepository;
import com.itercraft.api.domain.ludotheque.TypeJeu;
import com.itercraft.api.domain.ludotheque.TypeJeuRepository;
import com.itercraft.api.infrastructure.sse.SseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LudothequeServiceImpl implements LudothequeService {

    private static final Logger log = LoggerFactory.getLogger(LudothequeServiceImpl.class);
    private static final String SSE_EVENT_LUDOTHEQUE = "ludotheque-change";

    private final JeuRepository jeuRepository;
    private final JeuUserRepository jeuUserRepository;
    private final TypeJeuRepository typeJeuRepository;
    private final AgeJeuRepository ageJeuRepository;
    private final ComplexiteJeuRepository complexiteJeuRepository;
    private final ClaudeService claudeService;
    private final SseService sseService;
    private final String initUserEmail;

    public LudothequeServiceImpl(
            JeuRepository jeuRepository,
            JeuUserRepository jeuUserRepository,
            TypeJeuRepository typeJeuRepository,
            AgeJeuRepository ageJeuRepository,
            ComplexiteJeuRepository complexiteJeuRepository,
            ClaudeService claudeService,
            SseService sseService,
            @Value("${ludotheque.init-user-email:}") String initUserEmail) {
        this.jeuRepository = jeuRepository;
        this.jeuUserRepository = jeuUserRepository;
        this.typeJeuRepository = typeJeuRepository;
        this.ageJeuRepository = ageJeuRepository;
        this.complexiteJeuRepository = complexiteJeuRepository;
        this.claudeService = claudeService;
        this.sseService = sseService;
        this.initUserEmail = initUserEmail;
    }

    @Override
    @Transactional
    public List<JeuUserDto> getUserLudotheque(String userSub, String userEmail) {
        List<JeuUser> userJeux = jeuUserRepository.findByUserSub(userSub);

        // Auto-initialiser si c'est l'utilisateur configuré et sa collection est vide
        if (userJeux.isEmpty() && shouldAutoInitialize(userEmail)) {
            int added = doInitializeAllGames(userSub);
            if (added > 0) {
                userJeux = jeuUserRepository.findByUserSub(userSub);
            }
        }

        return userJeux.stream()
                .map(this::toJeuUserDto)
                .toList();
    }

    private boolean shouldAutoInitialize(String userEmail) {
        return initUserEmail != null
                && !initUserEmail.isBlank()
                && initUserEmail.equalsIgnoreCase(userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JeuDto> getAllJeux() {
        return jeuRepository.findAll().stream()
                .map(this::toJeuDto)
                .toList();
    }

    @Override
    @Transactional
    public void addJeuToUser(String userSub, UUID jeuId) {
        if (jeuUserRepository.findByUserSubAndJeuId(userSub, jeuId).isPresent()) {
            if (log.isInfoEnabled()) {
                log.info("Jeu {} déjà dans la ludothèque de {}", jeuId, sanitizeForLog(userSub));
            }
            return;
        }

        Jeu jeu = jeuRepository.findById(jeuId)
                .orElseThrow(() -> new IllegalArgumentException("Jeu non trouvé: " + jeuId));

        JeuUser jeuUser = new JeuUser(userSub, jeu);
        jeuUserRepository.save(jeuUser);
        if (log.isInfoEnabled()) {
            log.info("Jeu {} ajouté à la ludothèque de {}", jeuId, sanitizeForLog(userSub));
        }
    }

    @Override
    @Transactional
    public void removeJeuFromUser(String userSub, UUID jeuId) {
        jeuUserRepository.deleteByUserSubAndJeuId(userSub, jeuId);
        if (log.isInfoEnabled()) {
            log.info("Jeu {} retiré de la ludothèque de {}", jeuId, sanitizeForLog(userSub));
        }
        sseService.broadcast(SSE_EVENT_LUDOTHEQUE);
    }

    @Override
    @Transactional
    public void updateNote(String userSub, UUID jeuId, Short note) {
        JeuUser jeuUser = jeuUserRepository.findByUserSubAndJeuId(userSub, jeuId)
                .orElseThrow(() -> new IllegalArgumentException("Jeu non trouvé dans la ludothèque"));

        jeuUser.setNote(note);
        jeuUserRepository.save(jeuUser);
        if (log.isInfoEnabled()) {
            log.info("Note mise à jour pour jeu {} de {}", jeuId, sanitizeForLog(userSub));
        }
        sseService.broadcast(SSE_EVENT_LUDOTHEQUE);
    }

    @Override
    @Transactional
    public int initializeAllGames(String userSub) {
        return doInitializeAllGames(userSub);
    }

    private int doInitializeAllGames(String userSub) {
        List<Jeu> allJeux = jeuRepository.findAll();
        int added = 0;

        for (Jeu jeu : allJeux) {
            if (jeuUserRepository.findByUserSubAndJeuId(userSub, jeu.getId()).isEmpty()) {
                JeuUser jeuUser = new JeuUser(userSub, jeu);
                jeuUserRepository.save(jeuUser);
                added++;
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Ludothèque initialisée pour {}: {} jeux ajoutés sur {} disponibles",
                    sanitizeForLog(userSub), added, allJeux.size());
        }
        return added;
    }

    @Override
    @Transactional
    public JeuUserDto addJeuByTitle(String userSub, String nom) {
        // Vérifier si le jeu existe déjà
        Jeu jeu = jeuRepository.findByNomIgnoreCase(nom).orElse(null);

        if (jeu == null) {
            // Demander à Claude de remplir les informations
            GameInfoResponse gameInfo = claudeService.fillGameInfo(nom);

            TypeJeu typeJeu = typeJeuRepository.findByCode(gameInfo.typeCode())
                    .orElseGet(() -> typeJeuRepository.findByCode("ambiance").orElseThrow());
            AgeJeu ageJeu = ageJeuRepository.findByCode(gameInfo.ageCode())
                    .orElseGet(() -> ageJeuRepository.findByCode("tout_public").orElseThrow());
            ComplexiteJeu complexite = complexiteJeuRepository.findByNiveau(gameInfo.complexiteNiveau())
                    .orElseGet(() -> complexiteJeuRepository.findByNiveau((short) 2).orElseThrow());

            jeu = new Jeu.Builder()
                    .nom(gameInfo.nom())
                    .description(gameInfo.description())
                    .typeJeu(typeJeu)
                    .joueursMin(gameInfo.joueursMin())
                    .joueursMax(gameInfo.joueursMax())
                    .ageJeu(ageJeu)
                    .dureeMoyenneMinutes(gameInfo.dureeMoyenneMinutes())
                    .complexite(complexite)
                    .imageUrl(gameInfo.imageUrl())
                    .build();

            jeu = jeuRepository.save(jeu);
            if (log.isInfoEnabled()) {
                log.info("Nouveau jeu créé via Claude: {}", sanitizeForLog(jeu.getNom()));
            }
        }

        // Vérifier si l'utilisateur n'a pas déjà ce jeu
        if (jeuUserRepository.findByUserSubAndJeuId(userSub, jeu.getId()).isPresent()) {
            throw new IllegalArgumentException("Ce jeu est déjà dans votre ludothèque");
        }

        JeuUser jeuUser = new JeuUser(userSub, jeu);
        jeuUser = jeuUserRepository.save(jeuUser);
        if (log.isInfoEnabled()) {
            log.info("Jeu {} ajouté à la ludothèque de {}", jeu.getId(), sanitizeForLog(userSub));
        }

        sseService.broadcast(SSE_EVENT_LUDOTHEQUE);
        return toJeuUserDto(jeuUser);
    }

    @Override
    @Transactional(readOnly = true)
    public GameSuggestionDto getSuggestion(String userSub) {
        List<JeuUser> ratedGames = jeuUserRepository.findRatedByUserSub(userSub);

        if (ratedGames.isEmpty()) {
            throw new IllegalArgumentException("Vous devez noter au moins un jeu pour obtenir une suggestion");
        }

        List<RatedGameInfo> ratedGameInfos = ratedGames.stream()
                .map(ju -> new RatedGameInfo(
                        ju.getJeu().getNom(),
                        ju.getJeu().getTypeJeu().getCode(),
                        ju.getNote(),
                        ju.getJeu().getDescription()
                ))
                .toList();

        GameSuggestionResponse suggestion = claudeService.suggestGame(ratedGameInfos);

        String typeLibelle = typeJeuRepository.findByCode(suggestion.typeCode())
                .map(TypeJeu::getLibelle)
                .orElse(suggestion.typeCode());

        return new GameSuggestionDto(
                suggestion.nom(),
                suggestion.description(),
                suggestion.typeCode(),
                typeLibelle,
                suggestion.raison(),
                suggestion.imageUrl()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReferenceTablesDto getReferenceTables() {
        List<ReferenceTablesDto.TypeJeuRef> types = typeJeuRepository.findAll().stream()
                .map(t -> new ReferenceTablesDto.TypeJeuRef(t.getId(), t.getCode(), t.getLibelle()))
                .toList();

        List<ReferenceTablesDto.AgeJeuRef> ages = ageJeuRepository.findAll().stream()
                .map(a -> new ReferenceTablesDto.AgeJeuRef(a.getId(), a.getCode(), a.getLibelle(), a.getAgeMinimum()))
                .toList();

        List<ReferenceTablesDto.ComplexiteJeuRef> complexites = complexiteJeuRepository.findAll().stream()
                .map(c -> new ReferenceTablesDto.ComplexiteJeuRef(c.getId(), c.getNiveau(), c.getLibelle()))
                .toList();

        return new ReferenceTablesDto(types, ages, complexites);
    }

    private JeuDto toJeuDto(Jeu jeu) {
        return new JeuDto(
                jeu.getId(),
                jeu.getNom(),
                jeu.getDescription(),
                jeu.getTypeJeu().getCode(),
                jeu.getTypeJeu().getLibelle(),
                jeu.getJoueursMin(),
                jeu.getJoueursMax(),
                jeu.getAgeJeu().getCode(),
                jeu.getAgeJeu().getLibelle(),
                jeu.getDureeMoyenneMinutes(),
                jeu.getComplexite().getNiveau(),
                jeu.getComplexite().getLibelle(),
                jeu.getImageUrl()
        );
    }

    private JeuUserDto toJeuUserDto(JeuUser jeuUser) {
        return new JeuUserDto(
                jeuUser.getId(),
                toJeuDto(jeuUser.getJeu()),
                jeuUser.getNote(),
                jeuUser.getCreatedAt()
        );
    }

    private static String sanitizeForLog(String input) {
        if (input == null) return "null";
        return input.replaceAll("[\\r\\n\\t]", "_");
    }
}
