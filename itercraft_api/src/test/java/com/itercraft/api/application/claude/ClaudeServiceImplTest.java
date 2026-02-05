package com.itercraft.api.application.claude;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.util.List;
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

    @Test
    void fillGameInfo_shouldReturnGameInfo() {
        String responseJson = """
                {
                    "content": [
                        {
                            "type": "text",
                            "text": "{\\"nom\\": \\"Catan\\", \\"description\\": \\"Jeu de colonisation\\", \\"typeCode\\": \\"strategie\\", \\"joueursMin\\": 3, \\"joueursMax\\": 4, \\"ageCode\\": \\"tout_public\\", \\"dureeMoyenneMinutes\\": 90, \\"complexiteNiveau\\": 3, \\"imageUrl\\": null}"
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("x-api-key", "test-api-key"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        GameInfoResponse result = service.fillGameInfo("Catan");

        assertThat(result).isNotNull();
        assertThat(result.nom()).isEqualTo("Catan");
        assertThat(result.description()).isEqualTo("Jeu de colonisation");
        assertThat(result.typeCode()).isEqualTo("strategie");
        assertThat(result.joueursMin()).isEqualTo((short) 3);
        assertThat(result.joueursMax()).isEqualTo((short) 4);
        assertThat(result.ageCode()).isEqualTo("tout_public");
        assertThat(result.dureeMoyenneMinutes()).isEqualTo((short) 90);
        assertThat(result.complexiteNiveau()).isEqualTo((short) 3);
        mockServer.verify();
    }

    @Test
    void fillGameInfo_shouldReturnFallbackOnEmptyContent() {
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

        GameInfoResponse result = service.fillGameInfo("Unknown Game");

        assertThat(result).isNotNull();
        assertThat(result.nom()).isEqualTo("Unknown Game");
        assertThat(result.typeCode()).isEqualTo("ambiance");
    }

    @Test
    void fillGameInfo_shouldReturnFallbackOnInvalidJson() {
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

        GameInfoResponse result = service.fillGameInfo("Bad Game");

        assertThat(result).isNotNull();
        assertThat(result.nom()).isEqualTo("Bad Game");
        assertThat(result.description()).isEqualTo("Jeu de société");
    }

    @Test
    void fillGameInfo_shouldReturnFallbackOnParseException() {
        String invalidJson = "completely invalid";

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(invalidJson, MediaType.APPLICATION_JSON));

        GameInfoResponse result = service.fillGameInfo("Error Game");

        assertThat(result).isNotNull();
        assertThat(result.nom()).isEqualTo("Error Game");
    }

    @Test
    void suggestGame_shouldReturnSuggestion() {
        String responseJson = """
                {
                    "content": [
                        {
                            "type": "text",
                            "text": "{\\"nom\\": \\"Terraforming Mars\\", \\"description\\": \\"Colonisez Mars\\", \\"typeCode\\": \\"strategie\\", \\"raison\\": \\"Vous aimez les jeux de stratégie\\", \\"imageUrl\\": null}"
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("x-api-key", "test-api-key"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<RatedGameInfo> ratedGames = List.of(
                new RatedGameInfo("Catan", "strategie", (short) 4, "Jeu de colonisation")
        );
        GameSuggestionResponse result = service.suggestGame(ratedGames);

        assertThat(result).isNotNull();
        assertThat(result.nom()).isEqualTo("Terraforming Mars");
        assertThat(result.description()).isEqualTo("Colonisez Mars");
        assertThat(result.typeCode()).isEqualTo("strategie");
        assertThat(result.raison()).isEqualTo("Vous aimez les jeux de stratégie");
        mockServer.verify();
    }

    @Test
    void suggestGame_shouldReturnFallbackOnEmptyContent() {
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

        List<RatedGameInfo> ratedGames = List.of(
                new RatedGameInfo("Test", "ambiance", (short) 3, "Test game")
        );
        GameSuggestionResponse result = service.suggestGame(ratedGames);

        assertThat(result).isNotNull();
        assertThat(result.nom()).isEqualTo("Dixit");
        assertThat(result.typeCode()).isEqualTo("ambiance");
    }

    @Test
    void suggestGame_shouldReturnFallbackOnInvalidJson() {
        String responseJson = """
                {
                    "content": [
                        {
                            "type": "text",
                            "text": "invalid json content"
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<RatedGameInfo> ratedGames = List.of(
                new RatedGameInfo("Test", "strategie", (short) 5, "Test")
        );
        GameSuggestionResponse result = service.suggestGame(ratedGames);

        assertThat(result).isNotNull();
        assertThat(result.nom()).isEqualTo("Dixit");
    }

    @Test
    void suggestGame_shouldReturnFallbackOnParseException() {
        String invalidJson = "not json at all";

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(invalidJson, MediaType.APPLICATION_JSON));

        List<RatedGameInfo> ratedGames = List.of(
                new RatedGameInfo("Game", "reflexion", (short) 2, "Description")
        );
        GameSuggestionResponse result = service.suggestGame(ratedGames);

        assertThat(result).isNotNull();
        assertThat(result.nom()).isEqualTo("Dixit");
    }
}
