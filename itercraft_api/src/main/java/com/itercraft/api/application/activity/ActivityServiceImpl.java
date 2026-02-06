package com.itercraft.api.application.activity;

import com.itercraft.api.application.claude.ClaudeService;
import com.itercraft.api.application.meteo.MeteoService;
import com.itercraft.api.domain.activity.ActivityAnalysis;
import com.itercraft.api.domain.activity.ActivityAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ActivityServiceImpl implements ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityServiceImpl.class);

    private static final Map<String, String> WEATHER_LAYERS = Map.of(
            "Température", "TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND",
            "Précipitations", "TOTAL_PRECIPITATION_RATE__GROUND_OR_WATER_SURFACE",
            "Vent", "WIND_SPEED_GUST__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND",
            "Nuages", "LOW_CLOUD_COVER__GROUND_OR_WATER_SURFACE"
    );

    private final ActivityAnalysisRepository analysisRepository;
    private final MeteoService meteoService;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;

    public ActivityServiceImpl(
            ActivityAnalysisRepository analysisRepository,
            MeteoService meteoService,
            ClaudeService claudeService) {
        this.analysisRepository = analysisRepository;
        this.meteoService = meteoService;
        this.claudeService = claudeService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public ActivitySuggestion getSuggestions(double lat, double lon, String location) {
        LocalDate today = LocalDate.now();

        // Check cache first
        Optional<ActivityAnalysis> cached = analysisRepository.findByLocationNameAndAnalysisDate(location, today);
        if (cached.isPresent()) {
            if (log.isInfoEnabled()) {
                log.info("Cache hit for location={} date={}", sanitizeForLog(location), today);
            }
            return deserializeSuggestion(cached.get().getResponseJson(), location);
        }

        if (log.isInfoEnabled()) {
            log.info("Cache miss for location={} date={}, fetching from APIs", sanitizeForLog(location), today);
        }

        // Fetch weather images
        Map<String, byte[]> weatherImages = new LinkedHashMap<>();
        for (Map.Entry<String, String> layer : WEATHER_LAYERS.entrySet()) {
            byte[] image = meteoService.getMapImage(layer.getValue(), lat, lon, 512, 512);
            weatherImages.put(layer.getKey(), image);
        }

        // Get suggestions from Claude
        ActivitySuggestion suggestion = claudeService.suggestActivities(weatherImages, location, today);

        // Save to cache
        try {
            String json = objectMapper.writeValueAsString(suggestion);
            ActivityAnalysis analysis = new ActivityAnalysis(location, lat, lon, json);
            analysisRepository.save(analysis);
            if (log.isInfoEnabled()) {
                log.info("Cached analysis for location={} date={}", sanitizeForLog(location), today);
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to cache analysis for location={}: {}", sanitizeForLog(location), e.getMessage());
            }
        }

        return suggestion;
    }

    private ActivitySuggestion deserializeSuggestion(String json, String location) {
        try {
            return objectMapper.readValue(json, ActivitySuggestion.class);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to deserialize cached suggestion for location={}", sanitizeForLog(location), e);
            }
            return createFallbackSuggestion(location);
        }
    }

    private ActivitySuggestion createFallbackSuggestion(String location) {
        return new ActivitySuggestion(
                location,
                Map.of(
                        "morning", java.util.List.of(new Activity("Marche", "Profitez d'une balade matinale", "walk")),
                        "afternoon", java.util.List.of(new Activity("Lecture", "Moment de détente", "read")),
                        "evening", java.util.List.of(new Activity("Yoga", "Relaxation en fin de journée", "yoga"))
                ),
                "Suggestions par défaut - analyse indisponible."
        );
    }

    private static String sanitizeForLog(String input) {
        if (input == null) return "null";
        return input.replaceAll("[\\r\\n\\t]", "_");
    }
}
