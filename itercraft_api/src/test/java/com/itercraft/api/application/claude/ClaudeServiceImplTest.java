package com.itercraft.api.application.claude;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ClaudeServiceImplTest {

    private static final String TEMPERATURE_LAYER = "Température";

    private ClaudeServiceImpl service;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() throws Exception {
        service = new ClaudeServiceImpl("test-api-key", "claude-sonnet-4-20250514");

        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.anthropic.com");
        mockServer = MockRestServiceServer.bindTo(builder).build();

        RestClient mockRestClient = builder.build();
        Field restClientField = ClaudeServiceImpl.class.getDeclaredField("restClient");
        restClientField.setAccessible(true);
        restClientField.set(service, mockRestClient);
    }

    @Test
    void constructor_shouldCreateServiceWithoutError() {
        ClaudeServiceImpl newService = new ClaudeServiceImpl(
                "test-key", "claude-sonnet-4-20250514");
        assertThat(newService).isNotNull();
    }

    @Test
    void analyzeWeatherImage_shouldReturnAnalysis() {
        String responseJson = """
                {
                    "content": [
                        {
                            "type": "text",
                            "text": "Les températures sont douces sur Paris."
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("x-api-key", "test-api-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        byte[] imageData = new byte[]{0x01, 0x02, 0x03};
        String result = service.analyzeWeatherImage(imageData, "TEMPERATURE", "Paris");

        assertThat(result).isEqualTo("Les températures sont douces sur Paris.");
        mockServer.verify();
    }

    @Test
    void analyzeWeatherImage_shouldReturnDefaultWhenAnalysisIsBlank() {
        String responseJson = """
                {
                    "content": [
                        {
                            "type": "text",
                            "text": "   "
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        byte[] imageData = new byte[]{0x01, 0x02};
        String result = service.analyzeWeatherImage(imageData, "WIND", "Lyon");

        assertThat(result).isEqualTo("Analyse indisponible.");
    }

    @Test
    void analyzeWeatherImage_shouldReturnDefaultWhenAnalysisIsNull() {
        String responseJson = """
                {
                    "content": [
                        {
                            "type": "text"
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        byte[] imageData = new byte[]{0x01};
        String result = service.analyzeWeatherImage(imageData, "RAIN", "Marseille");

        assertThat(result).isEqualTo("Analyse indisponible.");
    }

    @Test
    void analyzeWeatherImage_shouldReturnDefaultOnParseError() {
        String invalidJson = "not valid json";

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(invalidJson, MediaType.APPLICATION_JSON));

        byte[] imageData = new byte[]{0x01};
        String result = service.analyzeWeatherImage(imageData, "CLOUD", "Nice");

        assertThat(result).isEqualTo("Analyse indisponible.");
    }

    @Test
    void suggestActivities_shouldReturnActivitySuggestion() {
        String responseJson = """
                {
                    "content": [
                        {
                            "type": "text",
                            "text": "{\\"summary\\": \\"Temps idéal pour sortir\\", \\"morning\\": [{\\"name\\": \\"Marche\\", \\"description\\": \\"Balade matinale\\", \\"icon\\": \\"walk\\"}], \\"afternoon\\": [{\\"name\\": \\"Piscine\\", \\"description\\": \\"Profitez\\", \\"icon\\": \\"pool\\"}], \\"evening\\": [{\\"name\\": \\"Yoga\\", \\"description\\": \\"Relaxation\\", \\"icon\\": \\"yoga\\"}]}"
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("x-api-key", "test-api-key"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        Map<String, byte[]> weatherImages = Map.of(
                TEMPERATURE_LAYER, new byte[]{0x01, 0x02, 0x03},
                "Précipitations", new byte[]{0x04, 0x05, 0x06}
        );
        var result = service.suggestActivities(weatherImages, "Paris");

        assertThat(result).isNotNull();
        assertThat(result.location()).isEqualTo("Paris");
        assertThat(result.summary()).isEqualTo("Temps idéal pour sortir");
        assertThat(result.activities().get("morning")).hasSize(1);
        assertThat(result.activities().get("morning").getFirst().name()).isEqualTo("Marche");
        mockServer.verify();
    }

    @Test
    void suggestActivities_shouldReturnFallbackOnInvalidJson() {
        String responseJson = """
                {
                    "content": [
                        {
                            "type": "text",
                            "text": "not valid json"
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        Map<String, byte[]> weatherImages = Map.of(TEMPERATURE_LAYER, new byte[]{0x01});
        var result = service.suggestActivities(weatherImages, "Lyon");

        assertThat(result).isNotNull();
        assertThat(result.location()).isEqualTo("Lyon");
        assertThat(result.summary()).contains("défaut");
        assertThat(result.activities().get("morning")).isNotEmpty();
    }

    @Test
    void suggestActivities_shouldReturnFallbackOnEmptyContent() {
        String responseJson = """
                {
                    "content": [
                        {
                            "type": "text",
                            "text": ""
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        Map<String, byte[]> weatherImages = Map.of(TEMPERATURE_LAYER, new byte[]{0x01});
        var result = service.suggestActivities(weatherImages, "Marseille");

        assertThat(result).isNotNull();
        assertThat(result.location()).isEqualTo("Marseille");
        assertThat(result.activities()).isNotEmpty();
    }
}
