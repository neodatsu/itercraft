package com.itercraft.api.application.activity;

import com.itercraft.api.application.claude.ClaudeService;
import com.itercraft.api.application.meteo.MeteoService;
import com.itercraft.api.domain.activity.ActivityAnalysis;
import com.itercraft.api.domain.activity.ActivityAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {

    private static final String LOCATION = "Paris";
    private static final double LAT = 48.8566;
    private static final double LON = 2.3522;

    @Mock
    private ActivityAnalysisRepository analysisRepository;

    @Mock
    private MeteoService meteoService;

    @Mock
    private ClaudeService claudeService;

    private ActivityServiceImpl activityService;

    @BeforeEach
    void setUp() {
        activityService = new ActivityServiceImpl(analysisRepository, meteoService, claudeService);
    }

    @Test
    void getSuggestions_shouldReturnCachedAnalysisWhenPresent() {
        String cachedJson = """
                {
                    "location": "Paris",
                    "activities": {
                        "morning": [{"name": "Marche", "description": "Balade", "icon": "walk"}],
                        "afternoon": [{"name": "Lecture", "description": "Détente", "icon": "read"}],
                        "evening": [{"name": "Yoga", "description": "Relaxation", "icon": "yoga"}]
                    },
                    "summary": "Journée ensoleillée"
                }
                """;
        ActivityAnalysis cachedAnalysis = new ActivityAnalysis(LOCATION, LAT, LON, cachedJson);

        when(analysisRepository.findByLocationNameAndAnalysisDate(LOCATION, LocalDate.now()))
                .thenReturn(Optional.of(cachedAnalysis));

        ActivitySuggestion result = activityService.getSuggestions(LAT, LON, LOCATION);

        assertThat(result).isNotNull();
        assertThat(result.location()).isEqualTo(LOCATION);
        assertThat(result.summary()).isEqualTo("Journée ensoleillée");
        assertThat(result.activities().get("morning")).hasSize(1);
        assertThat(result.activities().get("morning").getFirst().name()).isEqualTo("Marche");

        verify(analysisRepository).findByLocationNameAndAnalysisDate(LOCATION, LocalDate.now());
        verifyNoInteractions(meteoService);
        verifyNoInteractions(claudeService);
    }

    @Test
    void getSuggestions_shouldFetchFromApisWhenNoCacheHit() {
        byte[] fakeImage = new byte[]{0x01, 0x02, 0x03};
        ActivitySuggestion suggestion = new ActivitySuggestion(
                LOCATION,
                Map.of(
                        "morning", List.of(new Activity("Vélo", "Tour en vélo", "bike")),
                        "afternoon", List.of(new Activity("Piscine", "Baignade", "pool")),
                        "evening", List.of(new Activity("Cinéma", "Film", "movie"))
                ),
                "Temps variable"
        );

        when(analysisRepository.findByLocationNameAndAnalysisDate(LOCATION, LocalDate.now()))
                .thenReturn(Optional.empty());
        when(meteoService.getMapImage(anyString(), eq(LAT), eq(LON), eq(512), eq(512)))
                .thenReturn(fakeImage);
        when(claudeService.suggestActivities(anyMap(), eq(LOCATION)))
                .thenReturn(suggestion);

        ActivitySuggestion result = activityService.getSuggestions(LAT, LON, LOCATION);

        assertThat(result).isNotNull();
        assertThat(result.location()).isEqualTo(LOCATION);
        assertThat(result.summary()).isEqualTo("Temps variable");

        verify(meteoService, times(4)).getMapImage(anyString(), eq(LAT), eq(LON), eq(512), eq(512));
        verify(claudeService).suggestActivities(anyMap(), eq(LOCATION));
        verify(analysisRepository).save(any(ActivityAnalysis.class));
    }

    @Test
    void getSuggestions_shouldSaveAnalysisToCache() {
        byte[] fakeImage = new byte[]{0x01};
        ActivitySuggestion suggestion = new ActivitySuggestion(
                LOCATION,
                Map.of(
                        "morning", List.of(new Activity("Test", "Test", "test")),
                        "afternoon", List.of(new Activity("Test", "Test", "test")),
                        "evening", List.of(new Activity("Test", "Test", "test"))
                ),
                "Test summary"
        );

        when(analysisRepository.findByLocationNameAndAnalysisDate(LOCATION, LocalDate.now()))
                .thenReturn(Optional.empty());
        when(meteoService.getMapImage(anyString(), eq(LAT), eq(LON), eq(512), eq(512)))
                .thenReturn(fakeImage);
        when(claudeService.suggestActivities(anyMap(), eq(LOCATION)))
                .thenReturn(suggestion);

        activityService.getSuggestions(LAT, LON, LOCATION);

        ArgumentCaptor<ActivityAnalysis> captor = ArgumentCaptor.forClass(ActivityAnalysis.class);
        verify(analysisRepository).save(captor.capture());

        ActivityAnalysis saved = captor.getValue();
        assertThat(saved.getLocationName()).isEqualTo(LOCATION);
        assertThat(saved.getAnalysisDate()).isEqualTo(LocalDate.now());
        assertThat(saved.getResponseJson()).contains("Test summary");
    }

    @Test
    void getSuggestions_shouldReturnFallbackWhenCachedJsonIsInvalid() {
        String invalidJson = "not valid json";
        ActivityAnalysis cachedAnalysis = new ActivityAnalysis(LOCATION, LAT, LON, invalidJson);

        when(analysisRepository.findByLocationNameAndAnalysisDate(LOCATION, LocalDate.now()))
                .thenReturn(Optional.of(cachedAnalysis));

        ActivitySuggestion result = activityService.getSuggestions(LAT, LON, LOCATION);

        assertThat(result).isNotNull();
        assertThat(result.location()).isEqualTo(LOCATION);
        assertThat(result.summary()).contains("défaut");
        assertThat(result.activities()).isNotEmpty();

        verifyNoInteractions(meteoService);
        verifyNoInteractions(claudeService);
    }

    @Test
    void getSuggestions_shouldFetchAllFourWeatherLayers() {
        byte[] fakeImage = new byte[]{0x01};
        ActivitySuggestion suggestion = new ActivitySuggestion(
                LOCATION,
                Map.of(
                        "morning", List.of(new Activity("Test", "Test", "test")),
                        "afternoon", List.of(new Activity("Test", "Test", "test")),
                        "evening", List.of(new Activity("Test", "Test", "test"))
                ),
                "Test"
        );

        when(analysisRepository.findByLocationNameAndAnalysisDate(LOCATION, LocalDate.now()))
                .thenReturn(Optional.empty());
        when(meteoService.getMapImage(anyString(), eq(LAT), eq(LON), eq(512), eq(512)))
                .thenReturn(fakeImage);
        when(claudeService.suggestActivities(anyMap(), eq(LOCATION)))
                .thenReturn(suggestion);

        activityService.getSuggestions(LAT, LON, LOCATION);

        verify(meteoService).getMapImage("TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND", LAT, LON, 512, 512);
        verify(meteoService).getMapImage("TOTAL_PRECIPITATION_RATE__GROUND_OR_WATER_SURFACE", LAT, LON, 512, 512);
        verify(meteoService).getMapImage("WIND_SPEED_GUST__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND", LAT, LON, 512, 512);
        verify(meteoService).getMapImage("LOW_CLOUD_COVER__GROUND_OR_WATER_SURFACE", LAT, LON, 512, 512);
    }

    @Test
    void getSuggestions_shouldContinueWhenCacheSaveFails() {
        byte[] fakeImage = new byte[]{0x01};
        ActivitySuggestion suggestion = new ActivitySuggestion(
                LOCATION,
                Map.of(
                        "morning", List.of(new Activity("Test", "Test", "test")),
                        "afternoon", List.of(new Activity("Test", "Test", "test")),
                        "evening", List.of(new Activity("Test", "Test", "test"))
                ),
                "Test"
        );

        when(analysisRepository.findByLocationNameAndAnalysisDate(LOCATION, LocalDate.now()))
                .thenReturn(Optional.empty());
        when(meteoService.getMapImage(anyString(), eq(LAT), eq(LON), eq(512), eq(512)))
                .thenReturn(fakeImage);
        when(claudeService.suggestActivities(anyMap(), eq(LOCATION)))
                .thenReturn(suggestion);
        when(analysisRepository.save(any(ActivityAnalysis.class)))
                .thenThrow(new RuntimeException("Database error"));

        ActivitySuggestion result = activityService.getSuggestions(LAT, LON, LOCATION);

        assertThat(result).isNotNull();
        assertThat(result.summary()).isEqualTo("Test");
    }
}
