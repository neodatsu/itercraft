package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.sensor.SensorDataService;
import com.itercraft.api.infrastructure.security.SecurityConfig;
import com.itercraft.api.infrastructure.web.dto.SensorDataPointDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensorDataController.class)
@Import(SecurityConfig.class)
class SensorDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SensorDataService sensorDataService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String SUB = "user-sub-123";

    @Test
    void getSensorData_shouldReturnList() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        when(sensorDataService.getSensorData(eq(SUB), any(), any()))
                .thenReturn(List.of(new SensorDataPointDto(
                        now, "meteoStation_1", 20.7, 52.0, 21.1, 77.0)));

        mockMvc.perform(get("/api/sensors/data")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceName").value("meteoStation_1"))
                .andExpect(jsonPath("$[0].dhtTemperature").value(20.7))
                .andExpect(jsonPath("$[0].dhtHumidity").value(52.0))
                .andExpect(jsonPath("$[0].ntcTemperature").value(21.1))
                .andExpect(jsonPath("$[0].luminosity").value(77.0));
    }

    @Test
    void getSensorData_shouldAcceptDateRangeParams() throws Exception {
        when(sensorDataService.getSensorData(eq(SUB), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/sensors/data")
                        .param("from", "2026-02-01T00:00:00+01:00")
                        .param("to", "2026-02-08T00:00:00+01:00")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(sensorDataService).getSensorData(eq(SUB), any(), any());
    }

    @Test
    void getSensorData_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/sensors/data"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSensorData_shouldReturnEmptyListWhenNoData() throws Exception {
        when(sensorDataService.getSensorData(eq(SUB), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/sensors/data")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
