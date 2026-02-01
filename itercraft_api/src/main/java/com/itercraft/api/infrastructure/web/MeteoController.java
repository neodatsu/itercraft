package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.meteo.MeteoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meteo")
public class MeteoController {

    private final MeteoService meteoService;

    public MeteoController(MeteoService meteoService) {
        this.meteoService = meteoService;
    }

    @GetMapping(value = "/map", produces = MediaType.IMAGE_PNG_VALUE)
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
}
