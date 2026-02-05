package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.activity.Activity;
import com.itercraft.api.application.activity.ActivityService;
import com.itercraft.api.application.activity.ActivitySuggestion;
import com.itercraft.api.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivitiesController.class)
@Import(SecurityConfig.class)
class ActivitiesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void suggestActivities_shouldReturnActivitySuggestion() throws Exception {
        ActivitySuggestion suggestion = new ActivitySuggestion(
                "Paris",
                Map.of(
                        "morning", List.of(new Activity("Marche", "Balade matinale", "walk")),
                        "afternoon", List.of(new Activity("Piscine", "Profitez de la chaleur", "pool")),
                        "evening", List.of(new Activity("Yoga", "Relaxation", "yoga"))
                ),
                "Journée idéale pour les activités de plein air."
        );

        when(activityService.getSuggestions(48.8566, 2.3522, "Paris"))
                .thenReturn(suggestion);

        mockMvc.perform(post("/api/activities/suggest")
                        .param("lat", "48.8566")
                        .param("lon", "2.3522")
                        .param("location", "Paris")
                        .with(jwt())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Paris"))
                .andExpect(jsonPath("$.summary").value("Journée idéale pour les activités de plein air."))
                .andExpect(jsonPath("$.activities.morning[0].name").value("Marche"))
                .andExpect(jsonPath("$.activities.afternoon[0].name").value("Piscine"))
                .andExpect(jsonPath("$.activities.evening[0].name").value("Yoga"));

        verify(activityService).getSuggestions(48.8566, 2.3522, "Paris");
    }

    @Test
    void suggestActivities_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/activities/suggest")
                        .param("lat", "48.8566")
                        .param("lon", "2.3522")
                        .param("location", "Paris")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void suggestActivities_shouldReturn400WithInvalidLocation() throws Exception {
        mockMvc.perform(post("/api/activities/suggest")
                        .param("lat", "48.8566")
                        .param("lon", "2.3522")
                        .param("location", "<script>alert('xss')</script>")
                        .with(jwt())
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
