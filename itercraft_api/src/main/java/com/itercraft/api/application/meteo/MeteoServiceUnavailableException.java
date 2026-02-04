package com.itercraft.api.application.meteo;

/**
 * Exception thrown when the Météo France API is unavailable
 * (circuit breaker open or service down).
 */
public class MeteoServiceUnavailableException extends RuntimeException {

    public MeteoServiceUnavailableException(String message) {
        super(message);
    }

    public MeteoServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
