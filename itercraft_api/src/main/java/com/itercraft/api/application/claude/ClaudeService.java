package com.itercraft.api.application.claude;

import com.itercraft.api.application.activity.ActivitySuggestion;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ClaudeService {
    String analyzeWeatherImage(byte[] imageData, String layerLabel, String location);

    ActivitySuggestion suggestActivities(Map<String, byte[]> weatherImages, String location, LocalDate date);

    GameInfoResponse fillGameInfo(String gameTitle);

    GameSuggestionResponse suggestGame(List<RatedGameInfo> ratedGames);
}
