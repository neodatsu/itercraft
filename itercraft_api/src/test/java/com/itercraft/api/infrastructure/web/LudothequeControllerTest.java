package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.ludotheque.GameSuggestionDto;
import com.itercraft.api.application.ludotheque.JeuDto;
import com.itercraft.api.application.ludotheque.JeuUserDto;
import com.itercraft.api.application.ludotheque.LudothequeService;
import com.itercraft.api.application.ludotheque.ReferenceTablesDto;
import com.itercraft.api.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LudothequeController.class)
@Import(SecurityConfig.class)
class LudothequeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LudothequeService ludothequeService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String SUB = "user-sub-123";

    @Test
    void getAllJeux_shouldReturnList() throws Exception {
        JeuDto jeu = createJeuDto();
        when(ludothequeService.getAllJeux()).thenReturn(List.of(jeu));

        mockMvc.perform(get("/api/ludotheque/jeux")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Catan"));
    }

    @Test
    void getMesJeux_shouldReturnUserGames() throws Exception {
        JeuUserDto jeuUser = createJeuUserDto();
        when(ludothequeService.getUserLudotheque(eq(SUB), any())).thenReturn(List.of(jeuUser));

        mockMvc.perform(get("/api/ludotheque/mes-jeux")
                        .with(jwt().jwt(j -> j.subject(SUB).claim("email", "test@test.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jeu.nom").value("Catan"));
    }

    @Test
    void getMesJeux_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/ludotheque/mes-jeux"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addJeu_shouldReturn201() throws Exception {
        UUID jeuId = UUID.randomUUID();

        mockMvc.perform(post("/api/ludotheque/mes-jeux/" + jeuId)
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(ludothequeService).addJeuToUser(SUB, jeuId);
    }

    @Test
    void removeJeu_shouldReturn204() throws Exception {
        UUID jeuId = UUID.randomUUID();

        mockMvc.perform(delete("/api/ludotheque/mes-jeux/" + jeuId)
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(ludothequeService).removeJeuFromUser(SUB, jeuId);
    }

    @Test
    void updateNote_shouldReturn200() throws Exception {
        UUID jeuId = UUID.randomUUID();

        mockMvc.perform(put("/api/ludotheque/mes-jeux/" + jeuId + "/note")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\": 5}"))
                .andExpect(status().isOk());

        verify(ludothequeService).updateNote(eq(SUB), eq(jeuId), eq((short) 5));
    }

    @Test
    void initializeAll_shouldReturn200() throws Exception {
        when(ludothequeService.initializeAllGames(SUB)).thenReturn(10);

        mockMvc.perform(post("/api/ludotheque/mes-jeux/initialize-all")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeuxAjoutes").value(10));
    }

    @Test
    void addJeuByTitle_shouldReturn201() throws Exception {
        JeuUserDto jeuUser = createJeuUserDto();
        when(ludothequeService.addJeuByTitle(eq(SUB), any())).thenReturn(jeuUser);

        mockMvc.perform(post("/api/ludotheque/mes-jeux")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\": \"Catan\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jeu.nom").value("Catan"));
    }

    @Test
    void addJeuByTitle_shouldReturn400WhenNomBlank() throws Exception {
        mockMvc.perform(post("/api/ludotheque/mes-jeux")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSuggestion_shouldReturnSuggestion() throws Exception {
        GameSuggestionDto suggestion = new GameSuggestionDto(
                "Terraforming Mars", "Colonisez Mars", "strategie",
                "Stratégie", "Vous aimez la stratégie", null
        );
        when(ludothequeService.getSuggestion(SUB)).thenReturn(suggestion);

        mockMvc.perform(post("/api/ludotheque/suggestion")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Terraforming Mars"))
                .andExpect(jsonPath("$.raison").value("Vous aimez la stratégie"));
    }

    @Test
    void getReferences_shouldReturnReferences() throws Exception {
        ReferenceTablesDto references = new ReferenceTablesDto(
                List.of(new ReferenceTablesDto.TypeJeuRef(UUID.randomUUID(), "strategie", "Stratégie")),
                List.of(new ReferenceTablesDto.AgeJeuRef(UUID.randomUUID(), "tout_public", "Tout public", (short) 10)),
                List.of(new ReferenceTablesDto.ComplexiteJeuRef(UUID.randomUUID(), (short) 3, "Intermédiaire"))
        );
        when(ludothequeService.getReferenceTables()).thenReturn(references);

        mockMvc.perform(get("/api/ludotheque/references")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.types[0].code").value("strategie"))
                .andExpect(jsonPath("$.ages[0].code").value("tout_public"))
                .andExpect(jsonPath("$.complexites[0].niveau").value(3));
    }

    private JeuDto createJeuDto() {
        return new JeuDto(
                UUID.randomUUID(),
                "Catan",
                "Jeu de stratégie",
                "strategie",
                "Stratégie",
                (short) 3,
                (short) 4,
                "tout_public",
                "Tout public",
                (short) 90,
                (short) 3,
                "Intermédiaire",
                null
        );
    }

    private JeuUserDto createJeuUserDto() {
        return new JeuUserDto(
                UUID.randomUUID(),
                createJeuDto(),
                (short) 4,
                OffsetDateTime.now()
        );
    }
}
