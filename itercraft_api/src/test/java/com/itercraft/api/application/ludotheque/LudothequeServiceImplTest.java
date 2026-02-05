package com.itercraft.api.application.ludotheque;

import com.itercraft.api.application.claude.ClaudeService;
import com.itercraft.api.application.claude.GameInfoResponse;
import com.itercraft.api.application.claude.GameSuggestionResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LudothequeServiceImplTest {

    @Mock private JeuRepository jeuRepository;
    @Mock private JeuUserRepository jeuUserRepository;
    @Mock private TypeJeuRepository typeJeuRepository;
    @Mock private AgeJeuRepository ageJeuRepository;
    @Mock private ComplexiteJeuRepository complexiteJeuRepository;
    @Mock private ClaudeService claudeService;
    @Mock private SseService sseService;

    @InjectMocks
    private LudothequeServiceImpl ludothequeService;

    private static final String SUB = "user-sub-123";

    @Test
    void getUserLudotheque_shouldReturnUserGames() {
        Jeu jeu = createMockJeu();
        JeuUser jeuUser = new JeuUser(SUB, jeu);
        when(jeuUserRepository.findByUserSub(SUB)).thenReturn(List.of(jeuUser));

        List<JeuUserDto> result = ludothequeService.getUserLudotheque(SUB, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).jeu().nom()).isEqualTo("Catan");
    }

    @Test
    void getAllJeux_shouldReturnAllGames() {
        Jeu jeu = createMockJeu();
        when(jeuRepository.findAll()).thenReturn(List.of(jeu));

        List<JeuDto> result = ludothequeService.getAllJeux();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nom()).isEqualTo("Catan");
    }

    @Test
    void addJeuToUser_shouldAddGame() {
        UUID jeuId = UUID.randomUUID();
        Jeu jeu = createMockJeu();
        when(jeuUserRepository.findByUserSubAndJeuId(SUB, jeuId)).thenReturn(Optional.empty());
        when(jeuRepository.findById(jeuId)).thenReturn(Optional.of(jeu));
        when(jeuUserRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ludothequeService.addJeuToUser(SUB, jeuId);

        verify(jeuUserRepository).save(any(JeuUser.class));
    }

    @Test
    void addJeuToUser_shouldNotAddIfAlreadyOwned() {
        UUID jeuId = UUID.randomUUID();
        Jeu jeu = createMockJeu();
        JeuUser existing = new JeuUser(SUB, jeu);
        when(jeuUserRepository.findByUserSubAndJeuId(SUB, jeuId)).thenReturn(Optional.of(existing));

        ludothequeService.addJeuToUser(SUB, jeuId);

        // Should not throw and not save again
    }

    @Test
    void removeJeuFromUser_shouldRemoveAndBroadcast() {
        UUID jeuId = UUID.randomUUID();

        ludothequeService.removeJeuFromUser(SUB, jeuId);

        verify(jeuUserRepository).deleteByUserSubAndJeuId(SUB, jeuId);
        verify(sseService).broadcast("ludotheque-change");
    }

    @Test
    void updateNote_shouldUpdateAndBroadcast() {
        UUID jeuId = UUID.randomUUID();
        Jeu jeu = createMockJeu();
        JeuUser jeuUser = new JeuUser(SUB, jeu);
        when(jeuUserRepository.findByUserSubAndJeuId(SUB, jeuId)).thenReturn(Optional.of(jeuUser));
        when(jeuUserRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ludothequeService.updateNote(SUB, jeuId, (short) 5);

        assertThat(jeuUser.getNote()).isEqualTo((short) 5);
        verify(sseService).broadcast("ludotheque-change");
    }

    @Test
    void updateNote_shouldThrowWhenNotFound() {
        UUID jeuId = UUID.randomUUID();
        when(jeuUserRepository.findByUserSubAndJeuId(SUB, jeuId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ludothequeService.updateNote(SUB, jeuId, (short) 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addJeuByTitle_shouldCreateNewGameViaClaude() {
        String nom = "Nouveaux Jeu";
        GameInfoResponse gameInfo = new GameInfoResponse(
                nom, "Description", "strategie",
                (short) 2, (short) 4, "tout_public",
                (short) 60, (short) 3, null
        );

        when(jeuRepository.findByNomIgnoreCase(nom)).thenReturn(Optional.empty());
        when(claudeService.fillGameInfo(nom)).thenReturn(gameInfo);
        when(typeJeuRepository.findByCode("strategie")).thenReturn(Optional.of(createTypeJeu()));
        when(ageJeuRepository.findByCode("tout_public")).thenReturn(Optional.of(createAgeJeu()));
        when(complexiteJeuRepository.findByNiveau((short) 3)).thenReturn(Optional.of(createComplexiteJeu()));
        when(jeuRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jeuUserRepository.findByUserSubAndJeuId(any(), any())).thenReturn(Optional.empty());
        when(jeuUserRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        JeuUserDto result = ludothequeService.addJeuByTitle(SUB, nom);

        assertThat(result.jeu().nom()).isEqualTo(nom);
        verify(sseService).broadcast("ludotheque-change");
    }

    @Test
    void addJeuByTitle_shouldThrowIfAlreadyOwned() {
        String nom = "Catan";
        Jeu jeu = createMockJeu();
        JeuUser existing = new JeuUser(SUB, jeu);

        when(jeuRepository.findByNomIgnoreCase(nom)).thenReturn(Optional.of(jeu));
        when(jeuUserRepository.findByUserSubAndJeuId(SUB, jeu.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> ludothequeService.addJeuByTitle(SUB, nom))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà dans votre ludothèque");
    }

    @Test
    void getSuggestion_shouldReturnClaudeSuggestion() {
        Jeu jeu = createMockJeu();
        JeuUser jeuUser = new JeuUser(SUB, jeu);
        jeuUser.setNote((short) 4);

        GameSuggestionResponse suggestion = new GameSuggestionResponse(
                "Terraforming Mars", "Jeu de colonisation", "strategie",
                "Vous aimez les jeux de stratégie", null
        );

        when(jeuUserRepository.findRatedByUserSub(SUB)).thenReturn(List.of(jeuUser));
        when(claudeService.suggestGame(any())).thenReturn(suggestion);
        when(typeJeuRepository.findByCode("strategie")).thenReturn(Optional.of(createTypeJeu()));

        GameSuggestionDto result = ludothequeService.getSuggestion(SUB);

        assertThat(result.nom()).isEqualTo("Terraforming Mars");
        assertThat(result.raison()).isEqualTo("Vous aimez les jeux de stratégie");
    }

    @Test
    void getSuggestion_shouldThrowIfNoRatedGames() {
        when(jeuUserRepository.findRatedByUserSub(SUB)).thenReturn(List.of());

        assertThatThrownBy(() -> ludothequeService.getSuggestion(SUB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("noter au moins un jeu");
    }

    @Test
    void getReferenceTables_shouldReturnAllReferences() {
        when(typeJeuRepository.findAll()).thenReturn(List.of(createTypeJeu()));
        when(ageJeuRepository.findAll()).thenReturn(List.of(createAgeJeu()));
        when(complexiteJeuRepository.findAll()).thenReturn(List.of(createComplexiteJeu()));

        ReferenceTablesDto result = ludothequeService.getReferenceTables();

        assertThat(result.types()).hasSize(1);
        assertThat(result.ages()).hasSize(1);
        assertThat(result.complexites()).hasSize(1);
    }

    private Jeu createMockJeu() {
        TypeJeu typeJeu = createTypeJeu();
        AgeJeu ageJeu = createAgeJeu();
        ComplexiteJeu complexite = createComplexiteJeu();

        return new Jeu.Builder()
                .nom("Catan")
                .description("Jeu de stratégie")
                .typeJeu(typeJeu)
                .joueursMin((short) 3)
                .joueursMax((short) 4)
                .ageJeu(ageJeu)
                .dureeMoyenneMinutes((short) 90)
                .complexite(complexite)
                .imageUrl(null)
                .build();
    }

    private TypeJeu createTypeJeu() {
        return new TypeJeu(UUID.randomUUID(), "strategie", "Stratégie");
    }

    private AgeJeu createAgeJeu() {
        return new AgeJeu(UUID.randomUUID(), "tout_public", "Tout public", (short) 10);
    }

    private ComplexiteJeu createComplexiteJeu() {
        return new ComplexiteJeu(UUID.randomUUID(), (short) 3, "Intermédiaire");
    }
}
