package com.itercraft.api.application.meteo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class MeteoServiceImplTest {

    @Test
    void getMapImage_shouldCallApiWithCorrectParameters() {
        byte[] expectedImage = new byte[]{1, 2, 3};

        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.baseUrl(any(String.class))).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class), any(), any(), any(), any())).thenReturn(headersSpec);
        when(headersSpec.header(any(), any(String[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(byte[].class)).thenReturn(expectedImage);

        MeteoServiceImpl service = new MeteoServiceImpl(builder, "https://api.test", "test-token");

        byte[] result = service.getMapImage("TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND", 48.8566, 2.3522, 512, 512);

        assertThat(result).isEqualTo(expectedImage);
        verify(headersSpec).header("apikey", "test-token");
    }
}
