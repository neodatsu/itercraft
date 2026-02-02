package com.itercraft.api.application.ollama;

public interface OllamaService {

    String analyzeWeatherImage(byte[] imageData, String layerLabel, String location);
}
