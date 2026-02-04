package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.claude.ClaudeService;
import com.itercraft.api.application.meteo.MeteoService;
import com.itercraft.api.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MeteoController.class)
@Import(SecurityConfig.class)
class MeteoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeteoService meteoService;

    @MockitoBean
    private ClaudeService claudeService;

    @MockitoBean
    private OpaqueTokenIntrospector opaqueTokenIntrospector;

    @Test
    void getMap_shouldReturnPngImage() throws Exception {
        byte[] fakeImage = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG magic bytes
        when(meteoService.getMapImage(eq("TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND"),
                eq(48.8566), eq(2.3522), eq(512), eq(512)))
                .thenReturn(fakeImage);

        mockMvc.perform(post("/api/meteo/map")
                        .param("layer", "TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND")
                        .param("lat", "48.8566")
                        .param("lon", "2.3522")
                        .with(opaqueToken())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(fakeImage));

        verify(meteoService).getMapImage("TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND",
                48.8566, 2.3522, 512, 512);
    }

    @Test
    void getMap_withCustomDimensions_shouldUseProvidedValues() throws Exception {
        byte[] fakeImage = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        when(meteoService.getMapImage(anyString(), anyDouble(), anyDouble(), eq(1024), eq(768)))
                .thenReturn(fakeImage);

        mockMvc.perform(post("/api/meteo/map")
                        .param("layer", "WIND_SPEED_GUST__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND")
                        .param("lat", "45.0")
                        .param("lon", "5.0")
                        .param("width", "1024")
                        .param("height", "768")
                        .with(opaqueToken())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));

        verify(meteoService).getMapImage("WIND_SPEED_GUST__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND",
                45.0, 5.0, 1024, 768);
    }

    @Test
    void analyzeWeatherMap_shouldReturnAnalysis() throws Exception {
        byte[] fakeImage = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        String analysis = "Températures douces sur Paris avec quelques nuages.";

        when(meteoService.getMapImage(eq("TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND"),
                eq(48.8566), eq(2.3522), eq(512), eq(512)))
                .thenReturn(fakeImage);
        when(claudeService.analyzeWeatherImage(fakeImage, "TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND", "Paris"))
                .thenReturn(analysis);

        mockMvc.perform(post("/api/meteo/analyze")
                        .param("layer", "TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND")
                        .param("lat", "48.8566")
                        .param("lon", "2.3522")
                        .param("location", "Paris")
                        .with(opaqueToken())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").value(analysis));

        verify(meteoService).getMapImage("TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND",
                48.8566, 2.3522, 512, 512);
        verify(claudeService).analyzeWeatherImage(fakeImage, "TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND", "Paris");
    }

    @Test
    void getMap_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/meteo/map")
                        .param("layer", "TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND")
                        .param("lat", "48.8566")
                        .param("lon", "2.3522")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void analyzeWeatherMap_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/meteo/analyze")
                        .param("layer", "TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND")
                        .param("lat", "48.8566")
                        .param("lon", "2.3522")
                        .param("location", "Paris")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
