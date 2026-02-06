package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.maintenance.MaintenanceService;
import com.itercraft.api.infrastructure.web.dto.CreateActivityRequest;
import com.itercraft.api.infrastructure.web.dto.MaintenanceActivityDto;
import com.itercraft.api.infrastructure.web.dto.MaintenanceSessionDto;
import com.itercraft.api.infrastructure.web.dto.MaintenanceTotalsDto;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping("/activities")
    public ResponseEntity<List<MaintenanceActivityDto>> getActivities(JwtAuthenticationToken token) {
        return ResponseEntity.ok(maintenanceService.getActivities(extractSub(token)));
    }

    @PostMapping("/activities")
    public ResponseEntity<MaintenanceActivityDto> createActivity(@RequestBody CreateActivityRequest request,
                                                                  JwtAuthenticationToken token) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.createActivity(extractSub(token), request.label()));
    }

    @PostMapping("/activities/{serviceCode}/start")
    public ResponseEntity<MaintenanceSessionDto> startActivity(@PathVariable String serviceCode,
                                                               JwtAuthenticationToken token) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.startActivity(extractSub(token), serviceCode));
    }

    @PostMapping("/activities/{serviceCode}/stop")
    public ResponseEntity<MaintenanceSessionDto> stopActivity(@PathVariable String serviceCode,
                                                              JwtAuthenticationToken token) {
        return ResponseEntity.ok(maintenanceService.stopActivity(extractSub(token), serviceCode));
    }

    @GetMapping("/totals")
    public ResponseEntity<MaintenanceTotalsDto> getTotals(JwtAuthenticationToken token) {
        return ResponseEntity.ok(maintenanceService.getTotals(extractSub(token)));
    }

    @GetMapping("/activities/{serviceCode}/history")
    public ResponseEntity<List<MaintenanceSessionDto>> getHistory(@PathVariable String serviceCode,
                                                                  JwtAuthenticationToken token) {
        return ResponseEntity.ok(maintenanceService.getSessionHistory(extractSub(token), serviceCode));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID sessionId,
                                              JwtAuthenticationToken token) {
        maintenanceService.deleteSession(extractSub(token), sessionId);
        return ResponseEntity.noContent().build();
    }

    private String extractSub(JwtAuthenticationToken token) {
        String sub = token.getToken().getSubject();
        if (sub == null) {
            throw new IllegalStateException("JWT does not contain 'sub' claim");
        }
        return sub;
    }
}
