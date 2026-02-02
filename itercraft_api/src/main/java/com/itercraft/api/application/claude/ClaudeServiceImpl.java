package com.itercraft.api.application.claude;

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
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeServiceImpl implements ClaudeService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeServiceImpl.class);

    private final RestClient restClient;
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
    public String analyzeWeatherImage(byte[] imageData, String layerLabel, String location) {
        String base64Image = Base64.getEncoder().encodeToString(imageData);

        String prompt = "Tu es un météorologue. Analyse cette carte météo montrant "
                + layerLabel + " pour la zone de " + location
                + ". Décris les conditions météorologiques en 2-3 phrases en français.";

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 512,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
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

        log.info("Requesting Claude analysis: model={}, layer={}, location={}", model, layerLabel, location);

        byte[] responseBytes = restClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .body(requestBody)
                .retrieve()
                .body(byte[].class);

        String responseJson = new String(responseBytes, StandardCharsets.UTF_8);

        log.debug("Claude raw response: {}", responseJson);

        try {
            JsonNode node = objectMapper.readTree(responseJson);
            String analysis = node.path("content").get(0).path("text").stringValue();
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
}
