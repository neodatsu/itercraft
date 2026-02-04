package com.itercraft.api.application.meteo;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MeteoServiceImpl implements MeteoService {

    private static final Logger log = LoggerFactory.getLogger(MeteoServiceImpl.class);

    private final RestClient restClient;
    private final String apiToken;

    private static final double BBOX_OFFSET = 0.5;

    public MeteoServiceImpl(
            @Value("${meteofrance.api.base-url}") String baseUrl,
            @Value("${meteofrance.api.token}") String apiToken) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiToken = apiToken;
    }

    @Override
    @CircuitBreaker(name = "meteoFrance", fallbackMethod = "getMapImageFallback")
    @Retry(name = "meteoFrance")
    public byte[] getMapImage(String layer, double lat, double lon, int width, int height) {
        log.debug("Fetching weather map: layer={}, lat={}, lon={}", layer, lat, lon);

        double minLat = lat - BBOX_OFFSET;
        double maxLat = lat + BBOX_OFFSET;
        double minLon = lon - BBOX_OFFSET;
        double maxLon = lon + BBOX_OFFSET;
        String bbox = minLat + "," + minLon + "," + maxLat + "," + maxLon;

        byte[] result = restClient.get()
                .uri("/wms/MF-NWP-HIGHRES-AROMEPI-001-FRANCE-WMS/GetMap"
                        + "?service=WMS&version=1.3.0&request=GetMap"
                        + "&layers={layers}&crs=EPSG:4326&format=image/png&transparent=true"
                        + "&bbox={bbox}&width={width}&height={height}",
                        layer, bbox, width, height)
                .header("apikey", apiToken)
                .retrieve()
                .body(byte[].class);

        log.info("Weather map fetched successfully: {} bytes", result != null ? result.length : 0);
        return result;
    }

    /**
     * Fallback method when Météo France API is unavailable.
     * Returns a placeholder image or cached data.
     */
    @SuppressWarnings("unused")
    private byte[] getMapImageFallback(String layer, double lat, double lon, int width, int height, Exception e) {
        log.warn("Météo France API unavailable, circuit breaker activated: {}", e.getMessage());
        // Return empty byte array - the controller can handle this gracefully
        // In production, you might want to return a cached image or placeholder
        throw new MeteoServiceUnavailableException("Service météo temporairement indisponible", e);
    }
}
