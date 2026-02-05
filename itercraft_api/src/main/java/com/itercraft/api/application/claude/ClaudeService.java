package com.itercraft.api.application.claude;

import com.itercraft.api.application.activity.ActivitySuggestion;

import java.util.Map;

public interface ClaudeService {
    String analyzeWeatherImage(byte[] imageData, String layerLabel, String location);

    ActivitySuggestion suggestActivities(Map<String, byte[]> weatherImages, String location);
}
