package com.itercraft.api.application.ollama;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OllamaServiceImpl implements OllamaService {

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaServiceImpl(
            @Value("${ollama.api.base-url}") String baseUrl,
            @Value("${ollama.model}") String model) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
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

        String responseJson = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        try {
            JsonNode node = objectMapper.readTree(responseJson);
            return node.path("response").stringValue("Analyse indisponible.");
        } catch (Exception e) {
            return "Analyse indisponible.";
        }
    }
}
