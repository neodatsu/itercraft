package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.maintenance.MaintenanceService;
import com.itercraft.api.infrastructure.security.SecurityConfig;
import com.itercraft.api.infrastructure.web.dto.ActivityTotalsDto;
import com.itercraft.api.infrastructure.web.dto.MaintenanceActivityDto;
import com.itercraft.api.infrastructure.web.dto.MaintenanceSessionDto;
import com.itercraft.api.infrastructure.web.dto.MaintenanceTotalsDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaintenanceController.class)
@Import(SecurityConfig.class)
class MaintenanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaintenanceService maintenanceService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String SUB = "user-sub-123";

    @Test
    void getActivities_shouldReturnList() throws Exception {
        when(maintenanceService.getActivities(SUB))
                .thenReturn(List.of(new MaintenanceActivityDto(
                        "tondeuse", "Passer la tondeuse", false, null, 30)));

        mockMvc.perform(get("/api/maintenance/activities")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceCode").value("tondeuse"))
                .andExpect(jsonPath("$[0].serviceLabel").value("Passer la tondeuse"))
                .andExpect(jsonPath("$[0].totalMinutesToday").value(30));
    }

    @Test
    void createActivity_shouldReturn201() throws Exception {
        when(maintenanceService.createActivity(SUB, "Jardinage"))
                .thenReturn(new MaintenanceActivityDto("jardinage", "Jardinage", false, null, 0));

        mockMvc.perform(post("/api/maintenance/activities")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Jardinage\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceCode").value("jardinage"))
                .andExpect(jsonPath("$.serviceLabel").value("Jardinage"));
    }

    @Test
    void startActivity_shouldReturn201() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        UUID sessionId = UUID.randomUUID();
        when(maintenanceService.startActivity(SUB, "tondeuse"))
                .thenReturn(new MaintenanceSessionDto(sessionId, "tondeuse", "Passer la tondeuse",
                        now, null, null, false));

        mockMvc.perform(post("/api/maintenance/activities/tondeuse/start")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceCode").value("tondeuse"));
    }

    @Test
    void stopActivity_shouldReturn200() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        UUID sessionId = UUID.randomUUID();
        when(maintenanceService.stopActivity(SUB, "tondeuse"))
                .thenReturn(new MaintenanceSessionDto(sessionId, "tondeuse", "Passer la tondeuse",
                        now.minusMinutes(30), now, 30, false));

        mockMvc.perform(post("/api/maintenance/activities/tondeuse/stop")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(30));
    }

    @Test
    void getTotals_shouldReturnTotals() throws Exception {
        when(maintenanceService.getTotals(SUB))
                .thenReturn(new MaintenanceTotalsDto(60, 300, 1200, 5000,
                        List.of(new ActivityTotalsDto("tondeuse", "Passer la tondeuse", 60, 300, 1200, 5000))));

        mockMvc.perform(get("/api/maintenance/totals")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayMinutes").value(60))
                .andExpect(jsonPath("$.byActivity[0].serviceCode").value("tondeuse"));
    }

    @Test
    void getHistory_shouldReturnList() throws Exception {
        UUID sessionId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.now().minusHours(1);
        OffsetDateTime end = OffsetDateTime.now();
        when(maintenanceService.getSessionHistory(SUB, "tondeuse"))
                .thenReturn(List.of(new MaintenanceSessionDto(sessionId, "tondeuse", "Passer la tondeuse",
                        start, end, 60, false)));

        mockMvc.perform(get("/api/maintenance/activities/tondeuse/history")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].durationMinutes").value(60));
    }

    @Test
    void deleteSession_shouldReturn204() throws Exception {
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(delete("/api/maintenance/sessions/" + sessionId)
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(maintenanceService).deleteSession(SUB, sessionId);
    }

    @Test
    void getActivities_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/maintenance/activities"))
                .andExpect(status().isUnauthorized());
    }
}
