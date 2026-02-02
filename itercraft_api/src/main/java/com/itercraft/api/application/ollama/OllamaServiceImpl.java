package com.itercraft.api.application.ollama;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OllamaServiceImpl implements OllamaService {

    private static final Logger log = LoggerFactory.getLogger(OllamaServiceImpl.class);

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaServiceImpl(
            @Value("${ollama.api.base-url}") String baseUrl,
            @Value("${ollama.model}") String model) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(300));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
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
                "prompt", prompt,
                "images", List.of(base64Image),
                "stream", false
        );

        log.info("Requesting Ollama analysis: model={}, layer={}, location={}", model, layerLabel, location);

        byte[] responseBytes = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(byte[].class);

        String responseJson = new String(responseBytes, java.nio.charset.StandardCharsets.UTF_8);

        log.debug("Ollama raw response: {}", responseJson);

        try {
            JsonNode node = objectMapper.readTree(responseJson);
            String analysis = node.path("response").stringValue("Analyse indisponible.");
            log.info("Ollama analysis completed ({} chars)", analysis.length());
            return analysis;
        } catch (Exception e) {
            log.error("Failed to parse Ollama response", e);
            return "Analyse indisponible.";
        }
    }
}
