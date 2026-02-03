package com.itercraft.api.application.meteo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MeteoServiceImplTest {

    private static final String BASE_URL = "https://public-api.meteofrance.fr/public/aromepi/1.0";

    private MeteoServiceImpl service;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() throws Exception {
        service = new MeteoServiceImpl(BASE_URL, "test-api-token");

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();

        RestClient mockRestClient = builder.build();
        Field restClientField = MeteoServiceImpl.class.getDeclaredField("restClient");
        restClientField.setAccessible(true);
        restClientField.set(service, mockRestClient);
    }

    @Test
    void constructor_shouldCreateServiceWithoutError() {
        MeteoServiceImpl newService = new MeteoServiceImpl(BASE_URL, "test-token");
        assertThat(newService).isNotNull();
    }

    @Test
    void getMapImage_shouldReturnImageBytes() {
        byte[] expectedImage = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A};

        mockServer.expect(request -> {
                    String uri = request.getURI().toString();
                    assertThat(uri).contains("/wms/MF-NWP-HIGHRES-AROMEPI-001-FRANCE-WMS/GetMap");
                    assertThat(uri).contains("layers=TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND");
                    assertThat(uri).contains("width=512");
                    assertThat(uri).contains("height=512");
                })
                .andExpect(header("apikey", "test-api-token"))
                .andRespond(withSuccess(expectedImage, MediaType.IMAGE_PNG));

        byte[] result = service.getMapImage(
                "TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND",
                48.8566, 2.3522, 512, 512);

        assertThat(result).isEqualTo(expectedImage);
        mockServer.verify();
    }

    @Test
    void getMapImage_shouldCalculateBoundingBoxCorrectly() {
        byte[] expectedImage = new byte[]{0x01, 0x02};

        mockServer.expect(request -> {
                    String uri = request.getURI().toString();
                    assertThat(uri).contains("layers=WIND_SPEED__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND");
                    assertThat(uri).contains("width=1024");
                    assertThat(uri).contains("height=768");
                })
                .andRespond(withSuccess(expectedImage, MediaType.IMAGE_PNG));

        byte[] result = service.getMapImage(
                "WIND_SPEED__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND",
                45.0, 0.0, 1024, 768);

        assertThat(result).isEqualTo(expectedImage);
    }

    @Test
    void getMapImage_withDifferentLayer_shouldUseCorrectLayer() {
        byte[] expectedImage = new byte[]{0x03, 0x04};

        mockServer.expect(request -> {
                    String uri = request.getURI().toString();
                    assertThat(uri).contains("layers=TOTAL_PRECIPITATION__GROUND_OR_WATER_SURFACE");
                    assertThat(uri).contains("width=256");
                    assertThat(uri).contains("height=256");
                })
                .andRespond(withSuccess(expectedImage, MediaType.IMAGE_PNG));

        byte[] result = service.getMapImage(
                "TOTAL_PRECIPITATION__GROUND_OR_WATER_SURFACE",
                43.0, 3.0, 256, 256);

        assertThat(result).isEqualTo(expectedImage);
    }
}
