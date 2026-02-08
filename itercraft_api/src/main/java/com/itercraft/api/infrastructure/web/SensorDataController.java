package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.sensor.SensorDataService;
import com.itercraft.api.infrastructure.web.dto.SensorDataPointDto;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sensors")
public class SensorDataController {

    private final SensorDataService sensorDataService;

    public SensorDataController(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @GetMapping("/data")
    public ResponseEntity<List<SensorDataPointDto>> getSensorData(
            JwtAuthenticationToken token,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

        String sub = extractSub(token);

        OffsetDateTime end = to != null ? to : OffsetDateTime.now();
        OffsetDateTime start = from != null ? from : end.minusDays(7);

        return ResponseEntity.ok(sensorDataService.getSensorData(sub, start, end));
    }

    private String extractSub(JwtAuthenticationToken token) {
        String sub = token.getToken().getSubject();
        if (sub == null) {
            throw new IllegalStateException("JWT does not contain 'sub' claim");
        }
        return sub;
    }
}
