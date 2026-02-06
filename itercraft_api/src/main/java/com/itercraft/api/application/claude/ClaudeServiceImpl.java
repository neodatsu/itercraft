package com.itercraft.api.application.claude;

import com.itercraft.api.application.activity.Activity;
import com.itercraft.api.application.activity.ActivitySuggestion;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeServiceImpl implements ClaudeService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeServiceImpl.class);
    private static final String CONTENT = "content";
    private static final String MORNING = "morning";
    private static final String AFTERNOON = "afternoon";
    private static final String EVENING = "evening";
    private static final String MODEL_KEY = "model";
    private static final String MAX_TOKENS_KEY = "max_tokens";
    private static final String MESSAGES_KEY = "messages";
    private static final String MESSAGES_URI = "/v1/messages";
    private static final String API_KEY_HEADER = "x-api-key";
    private static final String ANTHROPIC_VERSION_HEADER = "anthropic-version";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String DESCRIPTION_KEY = "description";
    private static final String IMAGE_URL_KEY = "imageUrl";

    private final RestClient restClient;

    private static String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }
        return input.replaceAll("[\\r\\n\\t]", "_");
    }
    private final String model;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClaudeServiceImpl(
            @Value("${anthropic.api.key}") String apiKey,
            @Value("${anthropic.model}") String model) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    @CircuitBreaker(name = "claude", fallbackMethod = "analyzeWeatherImageFallback")
    @SuppressWarnings("java:S2629") // SLF4J parameterized logging is already efficient
    public String analyzeWeatherImage(byte[] imageData, String layerLabel, String location) {
        String base64Image = Base64.getEncoder().encodeToString(imageData);

        String prompt = "Tu es un météorologue. Analyse cette carte météo montrant "
                + layerLabel + " pour la zone de " + location
                + ". Décris les conditions météorologiques en 2-3 phrases en français.";

        Map<String, Object> requestBody = Map.of(
                MODEL_KEY, model,
                MAX_TOKENS_KEY, 512,
                MESSAGES_KEY, List.of(Map.of(
                        "role", "user",
                        CONTENT, List.of(
                                Map.of(
                                        "type", "image",
                                        "source", Map.of(
                                                "type", "base64",
                                                "media_type", "image/png",
                                                "data", base64Image
                                        )
                                ),
                                Map.of(
                                        "type", "text",
                                        "text", prompt
                                )
                        )
                ))
        );

        log.info("Requesting Claude analysis: model={}, layer={}, location={}", model, sanitizeForLog(layerLabel), sanitizeForLog(location));

        byte[] responseBytes = restClient.post()
                .uri(MESSAGES_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER, apiKey)
                .header(ANTHROPIC_VERSION_HEADER, ANTHROPIC_VERSION)
                .body(requestBody)
                .retrieve()
                .body(byte[].class);

        String responseJson = new String(responseBytes, StandardCharsets.UTF_8);

        log.debug("Claude raw response: {}", responseJson);

        try {
            JsonNode node = objectMapper.readTree(responseJson);
            String analysis = node.path(CONTENT).get(0).path("text").stringValue();
            if (analysis == null || analysis.isBlank()) {
                return "Analyse indisponible.";
            }
            log.info("Claude analysis completed ({} chars)", analysis.length());
            return analysis;
        } catch (Exception e) {
            log.error("Failed to parse Claude response", e);
            return "Analyse indisponible.";
        }
    }

    /**
     * Fallback method when Claude API is unavailable.
     * Returns a user-friendly message instead of failing.
     */
    @SuppressWarnings("unused")
    private String analyzeWeatherImageFallback(byte[] imageData, String layerLabel, String location, Exception e) {
        log.warn("Claude API unavailable, circuit breaker activated: {}", e.getMessage());
        return "Service d'analyse IA temporairement indisponible. Veuillez réessayer dans quelques instants.";
    }

    @Override
    @CircuitBreaker(name = "claude", fallbackMethod = "suggestActivitiesFallback")
    @SuppressWarnings("java:S2629")
    public ActivitySuggestion suggestActivities(Map<String, byte[]> weatherImages, String location) {
        // Build content list with all weather images
        List<Map<String, Object>> contentList = new ArrayList<>();

        for (Map.Entry<String, byte[]> entry : weatherImages.entrySet()) {
            String layerLabel = entry.getKey();
            String base64Image = Base64.getEncoder().encodeToString(entry.getValue());

            contentList.add(Map.of(
                    "type", "image",
                    "source", Map.of(
                            "type", "base64",
                            "media_type", "image/png",
                            "data", base64Image
                    )
            ));
            contentList.add(Map.of(
                    "type", "text",
                    "text", "Carte météo: " + layerLabel
            ));
        }

        String prompt = """
                Tu es un conseiller en activités de plein air. Analyse ces cartes météo pour la zone de %s.
                Les cartes montrent: température, précipitations, vent et couverture nuageuse.

                IMPORTANT: Prends en compte TOUTES les conditions météo pour tes suggestions:
                - S'il pleut ou s'il y a des précipitations, privilégie les activités d'intérieur
                - S'il y a du vent fort, évite les activités comme le vélo ou le pique-nique
                - S'il fait très chaud, suggère la piscine ou des activités à l'ombre
                - S'il fait froid, suggère des activités d'intérieur ou bien couvertes

                Réponds UNIQUEMENT en JSON valide avec cette structure exacte (sans commentaires):
                {
                  "summary": "Résumé en une phrase des conditions et recommandations",
                  "morning": [{"name": "Nom activité", "description": "Description courte", "icon": "icone"}],
                  "afternoon": [{"name": "Nom activité", "description": "Description courte", "icon": "icone"}],
                  "evening": [{"name": "Nom activité", "description": "Description courte", "icon": "icone"}]
                }

                Icônes disponibles: bike, walk, pool, home, run, hike, picnic, garden, read, yoga, cinema
                Activités possibles: Balade à vélo, Marche, Piscine, Rester à la maison, Course à pied, Randonnée, Pique-nique, Jardinage, Lecture, Yoga, Cinéma

                Adapte tes suggestions aux conditions météo observées. 2-3 activités par créneau maximum.
                """.formatted(location);

        contentList.add(Map.of("type", "text", "text", prompt));

        Map<String, Object> requestBody = Map.of(
                MODEL_KEY, model,
                MAX_TOKENS_KEY, 1024,
                MESSAGES_KEY, List.of(Map.of(
                        "role", "user",
                        CONTENT, contentList
                ))
        );

        log.info("Requesting Claude activity suggestions: model={}, location={}, layers={}", model, sanitizeForLog(location), weatherImages.size());

        byte[] responseBytes = restClient.post()
                .uri(MESSAGES_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER, apiKey)
                .header(ANTHROPIC_VERSION_HEADER, ANTHROPIC_VERSION)
                .body(requestBody)
                .retrieve()
                .body(byte[].class);

        String responseJson = new String(responseBytes, StandardCharsets.UTF_8);
        log.debug("Claude raw response for activities: {}", responseJson);

        try {
            JsonNode node = objectMapper.readTree(responseJson);
            String content = node.path(CONTENT).get(0).path("text").stringValue();
            if (content == null || content.isBlank()) {
                return createFallbackSuggestion(location);
            }

            return parseActivitySuggestion(content, location);
        } catch (Exception e) {
            log.error("Failed to parse Claude activity response", e);
            return createFallbackSuggestion(location);
        }
    }

    private ActivitySuggestion parseActivitySuggestion(String jsonContent, String location) {
        try {
            // Extract JSON from response (Claude might add text around it)
            int startIndex = jsonContent.indexOf('{');
            int endIndex = jsonContent.lastIndexOf('}');
            if (startIndex == -1 || endIndex == -1) {
                return createFallbackSuggestion(location);
            }
            String jsonOnly = jsonContent.substring(startIndex, endIndex + 1);

            JsonNode root = objectMapper.readTree(jsonOnly);
            String summary = root.path("summary").stringValue();

            Map<String, List<Activity>> activities = new HashMap<>();
            activities.put(MORNING, parseActivities(root.path(MORNING)));
            activities.put(AFTERNOON, parseActivities(root.path(AFTERNOON)));
            activities.put(EVENING, parseActivities(root.path(EVENING)));

            if (log.isInfoEnabled()) {
                log.info("Activity suggestions parsed successfully for {}", sanitizeForLog(location));
            }
            return new ActivitySuggestion(location, activities, summary);
        } catch (Exception e) {
            log.error("Failed to parse activity JSON", e);
            return createFallbackSuggestion(location);
        }
    }

    private List<Activity> parseActivities(JsonNode arrayNode) {
        List<Activity> activities = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode actNode : arrayNode) {
                String name = actNode.path("name").stringValue();
                String description = actNode.path(DESCRIPTION_KEY).stringValue();
                String icon = actNode.path("icon").stringValue();
                if (name != null && !name.isBlank()) {
                    activities.add(new Activity(name, description != null ? description : "", icon != null ? icon : "walk"));
                }
            }
        }
        return activities;
    }

    private ActivitySuggestion createFallbackSuggestion(String location) {
        Map<String, List<Activity>> activities = new HashMap<>();
        activities.put(MORNING, List.of(new Activity("Marche", "Profitez d'une balade matinale", "walk")));
        activities.put(AFTERNOON, List.of(new Activity("Lecture", "Moment de détente", "read")));
        activities.put(EVENING, List.of(new Activity("Yoga", "Relaxation en fin de journée", "yoga")));
        return new ActivitySuggestion(location, activities, "Suggestions par défaut - analyse météo indisponible.");
    }

    @SuppressWarnings("unused")
    private ActivitySuggestion suggestActivitiesFallback(Map<String, byte[]> weatherImages, String location, Exception e) {
        log.warn("Claude API unavailable for activities, circuit breaker activated: {}", e.getMessage());
        return createFallbackSuggestion(location);
    }

    @Override
    @CircuitBreaker(name = "claude", fallbackMethod = "fillGameInfoFallback")
    @SuppressWarnings("java:S2629")
    public GameInfoResponse fillGameInfo(String gameTitle) {
        String prompt = """
                Tu es un expert en jeux de société. Pour le jeu "%s", fournis les informations suivantes.

                Réponds UNIQUEMENT en JSON valide avec cette structure exacte:
                {
                  "nom": "nom exact et complet du jeu",
                  "description": "description en français (2-3 phrases)",
                  "typeCode": "un parmi: lettres_mots, strategie, enquete_escape, culture_quiz, ambiance, classique, reflexion, adresse",
                  "joueursMin": nombre minimum de joueurs (entier),
                  "joueursMax": nombre maximum de joueurs (entier),
                  "ageCode": "un parmi: enfant, tout_public, adulte",
                  "dureeMoyenneMinutes": durée moyenne en minutes (entier),
                  "complexiteNiveau": niveau de 1 à 5 (1=très facile, 5=expert),
                  "imageUrl": null
                }
                """.formatted(gameTitle);

        Map<String, Object> requestBody = Map.of(
                MODEL_KEY, model,
                MAX_TOKENS_KEY, 512,
                MESSAGES_KEY, List.of(Map.of(
                        "role", "user",
                        CONTENT, List.of(Map.of("type", "text", "text", prompt))
                ))
        );

        log.info("Requesting Claude game info: model={}, title={}", model, sanitizeForLog(gameTitle));

        byte[] responseBytes = restClient.post()
                .uri(MESSAGES_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER, apiKey)
                .header(ANTHROPIC_VERSION_HEADER, ANTHROPIC_VERSION)
                .body(requestBody)
                .retrieve()
                .body(byte[].class);

        String responseJson = new String(responseBytes, StandardCharsets.UTF_8);
        log.debug("Claude raw response for game info: {}", responseJson);

        try {
            JsonNode node = objectMapper.readTree(responseJson);
            String content = node.path(CONTENT).get(0).path("text").stringValue();
            if (content == null || content.isBlank()) {
                return createFallbackGameInfo(gameTitle);
            }
            return parseGameInfoResponse(content, gameTitle);
        } catch (Exception e) {
            log.error("Failed to parse Claude game info response", e);
            return createFallbackGameInfo(gameTitle);
        }
    }

    private GameInfoResponse parseGameInfoResponse(String jsonContent, String gameTitle) {
        try {
            int startIndex = jsonContent.indexOf('{');
            int endIndex = jsonContent.lastIndexOf('}');
            if (startIndex == -1 || endIndex == -1) {
                return createFallbackGameInfo(gameTitle);
            }
            String jsonOnly = jsonContent.substring(startIndex, endIndex + 1);
            JsonNode root = objectMapper.readTree(jsonOnly);

            return new GameInfoResponse(
                    root.path("nom").stringValue(),
                    root.path(DESCRIPTION_KEY).stringValue(),
                    root.path("typeCode").stringValue(),
                    root.path("joueursMin").shortValue(),
                    root.path("joueursMax").shortValue(),
                    root.path("ageCode").stringValue(),
                    root.path("dureeMoyenneMinutes").isNull() ? null : root.path("dureeMoyenneMinutes").shortValue(),
                    root.path("complexiteNiveau").shortValue(),
                    root.path(IMAGE_URL_KEY).isNull() ? null : root.path(IMAGE_URL_KEY).stringValue()
            );
        } catch (Exception e) {
            log.error("Failed to parse game info JSON", e);
            return createFallbackGameInfo(gameTitle);
        }
    }

    private GameInfoResponse createFallbackGameInfo(String gameTitle) {
        return new GameInfoResponse(
                gameTitle,
                "Jeu de société",
                "ambiance",
                (short) 2,
                (short) 6,
                "tout_public",
                (short) 30,
                (short) 2,
                null
        );
    }

    @SuppressWarnings("unused")
    private GameInfoResponse fillGameInfoFallback(String gameTitle, Exception e) {
        log.warn("Claude API unavailable for game info, circuit breaker activated: {}", e.getMessage());
        return createFallbackGameInfo(gameTitle);
    }

    @Override
    @CircuitBreaker(name = "claude", fallbackMethod = "suggestGameFallback")
    @SuppressWarnings("java:S2629")
    public GameSuggestionResponse suggestGame(List<RatedGameInfo> ratedGames) {
        StringBuilder gamesContext = new StringBuilder();
        for (RatedGameInfo game : ratedGames) {
            gamesContext.append("- ").append(game.nom())
                    .append(" (").append(game.typeCode()).append("): ")
                    .append(game.note()).append("/5 étoiles\n");
        }

        String prompt = """
                Tu es un conseiller en jeux de société. Voici les jeux notés par l'utilisateur:
                %s

                En analysant ses préférences (types de jeux aimés, notes attribuées), suggère UN nouveau jeu qui correspondrait à ses goûts.
                Le jeu suggéré doit être différent de ceux listés ci-dessus.

                Réponds UNIQUEMENT en JSON valide avec cette structure exacte:
                {
                  "nom": "nom du jeu suggéré",
                  "description": "description en français (2-3 phrases)",
                  "typeCode": "un parmi: lettres_mots, strategie, enquete_escape, culture_quiz, ambiance, classique, reflexion, adresse",
                  "raison": "explication courte de pourquoi ce jeu conviendrait (basé sur ses notes)",
                  "imageUrl": null
                }
                """.formatted(gamesContext.toString());

        Map<String, Object> requestBody = Map.of(
                MODEL_KEY, model,
                MAX_TOKENS_KEY, 512,
                MESSAGES_KEY, List.of(Map.of(
                        "role", "user",
                        CONTENT, List.of(Map.of("type", "text", "text", prompt))
                ))
        );

        log.info("Requesting Claude game suggestion: model={}, ratedGames={}", model, ratedGames.size());

        byte[] responseBytes = restClient.post()
                .uri(MESSAGES_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER, apiKey)
                .header(ANTHROPIC_VERSION_HEADER, ANTHROPIC_VERSION)
                .body(requestBody)
                .retrieve()
                .body(byte[].class);

        String responseJson = new String(responseBytes, StandardCharsets.UTF_8);
        log.debug("Claude raw response for game suggestion: {}", responseJson);

        try {
            JsonNode node = objectMapper.readTree(responseJson);
            String content = node.path(CONTENT).get(0).path("text").stringValue();
            if (content == null || content.isBlank()) {
                return createFallbackGameSuggestion();
            }
            return parseGameSuggestionResponse(content);
        } catch (Exception e) {
            log.error("Failed to parse Claude game suggestion response", e);
            return createFallbackGameSuggestion();
        }
    }

    private GameSuggestionResponse parseGameSuggestionResponse(String jsonContent) {
        try {
            int startIndex = jsonContent.indexOf('{');
            int endIndex = jsonContent.lastIndexOf('}');
            if (startIndex == -1 || endIndex == -1) {
                return createFallbackGameSuggestion();
            }
            String jsonOnly = jsonContent.substring(startIndex, endIndex + 1);
            JsonNode root = objectMapper.readTree(jsonOnly);

            return new GameSuggestionResponse(
                    root.path("nom").stringValue(),
                    root.path(DESCRIPTION_KEY).stringValue(),
                    root.path("typeCode").stringValue(),
                    root.path("raison").stringValue(),
                    root.path(IMAGE_URL_KEY).isNull() ? null : root.path(IMAGE_URL_KEY).stringValue()
            );
        } catch (Exception e) {
            log.error("Failed to parse game suggestion JSON", e);
            return createFallbackGameSuggestion();
        }
    }

    private GameSuggestionResponse createFallbackGameSuggestion() {
        return new GameSuggestionResponse(
                "Dixit",
                "Jeu d'imagination et de créativité où les joueurs doivent deviner des cartes illustrées.",
                "ambiance",
                "Un classique apprécié pour son originalité et son accessibilité.",
                null
        );
    }

    @SuppressWarnings("unused")
    private GameSuggestionResponse suggestGameFallback(List<RatedGameInfo> ratedGames, Exception e) {
        log.warn("Claude API unavailable for game suggestion, circuit breaker activated: {}", e.getMessage());
        return createFallbackGameSuggestion();
    }
}
