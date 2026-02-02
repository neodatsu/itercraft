package com.itercraft.api.application.claude;

public interface ClaudeService {
    String analyzeWeatherImage(byte[] imageData, String layerLabel, String location);
}
