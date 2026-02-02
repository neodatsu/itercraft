package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.meteo.MeteoService;
import com.itercraft.api.application.ollama.OllamaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/meteo")
public class MeteoController {

    private final MeteoService meteoService;
    private final OllamaService ollamaService;

    public MeteoController(MeteoService meteoService, OllamaService ollamaService) {
        this.meteoService = meteoService;
        this.ollamaService = ollamaService;
    }

    @PostMapping(value = "/map", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getMap(
            @RequestParam String layer,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "512") int width,
            @RequestParam(defaultValue = "512") int height) {
        byte[] image = meteoService.getMapImage(layer, lat, lon, width, height);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> analyzeWeatherMap(
            @RequestParam String layer,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam String location) {
        byte[] image = meteoService.getMapImage(layer, lat, lon, 256, 256);
        String analysis = ollamaService.analyzeWeatherImage(image, layer, location);
        return ResponseEntity.ok(Map.of("analysis", analysis));
    }
}
