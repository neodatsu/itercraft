package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.meteo.MeteoService;
import com.itercraft.api.application.claude.ClaudeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/meteo")
public class MeteoController {

    private static final Set<String> ALLOWED_LAYERS = Set.of(
            "TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND",
            "TOTAL_PRECIPITATION__GROUND_OR_WATER_SURFACE",
            "TOTAL_PRECIPITATION_RATE__GROUND_OR_WATER_SURFACE",
            "WIND_SPEED_GUST__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND",
            "RELATIVE_HUMIDITY__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND",
            "LOW_CLOUD_COVER__GROUND_OR_WATER_SURFACE"
    );

    private static final Pattern LOCATION_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\s,'-]{1,100}$");

    private final MeteoService meteoService;
    private final ClaudeService ollamaService;

    public MeteoController(MeteoService meteoService, ClaudeService ollamaService) {
        this.meteoService = meteoService;
        this.ollamaService = ollamaService;
    }

    private static String validateLayer(String layer) {
        if (layer == null || !ALLOWED_LAYERS.contains(layer)) {
            throw new IllegalArgumentException("Invalid layer parameter");
        }
        return layer;
    }

    private static String validateLocation(String location) {
        if (location == null || !LOCATION_PATTERN.matcher(location).matches()) {
            throw new IllegalArgumentException("Invalid location parameter");
        }
        return location;
    }

    @PostMapping(value = "/map", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getMap(
            @RequestParam String layer,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "512") int width,
            @RequestParam(defaultValue = "512") int height) {
        String validatedLayer = validateLayer(layer);
        byte[] image = meteoService.getMapImage(validatedLayer, lat, lon, width, height);
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
        String validatedLayer = validateLayer(layer);
        String validatedLocation = validateLocation(location);
        byte[] image = meteoService.getMapImage(validatedLayer, lat, lon, 512, 512);
        String analysis = ollamaService.analyzeWeatherImage(image, validatedLayer, validatedLocation);
        return ResponseEntity.ok(Map.of("analysis", analysis));
    }
}
