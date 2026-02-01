package com.itercraft.api.application.meteo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MeteoServiceImpl implements MeteoService {

    private final RestClient restClient;
    private final String apiToken;

    private static final double BBOX_OFFSET = 0.5;

    public MeteoServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${meteofrance.api.base-url}") String baseUrl,
            @Value("${meteofrance.api.token}") String apiToken) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiToken = apiToken;
    }

    @Override
    public byte[] getMapImage(String layer, double lat, double lon, int width, int height) {
        double minLat = lat - BBOX_OFFSET;
        double maxLat = lat + BBOX_OFFSET;
        double minLon = lon - BBOX_OFFSET;
        double maxLon = lon + BBOX_OFFSET;
        String bbox = minLat + "," + minLon + "," + maxLat + "," + maxLon;

        return restClient.get()
                .uri("/wms/MF-NWP-HIGHRES-AROMEPI-001-FRANCE-WMS/GetMap"
                        + "?service=WMS&version=1.3.0&request=GetMap"
                        + "&layers={layers}&crs=EPSG:4326&format=image/png&transparent=true"
                        + "&bbox={bbox}&width={width}&height={height}",
                        layer, bbox, width, height)
                .header("apikey", apiToken)
                .retrieve()
                .body(byte[].class);
    }
}
