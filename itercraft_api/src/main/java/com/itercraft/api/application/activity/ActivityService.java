package com.itercraft.api.application.activity;

public interface ActivityService {
    ActivitySuggestion getSuggestions(double lat, double lon, String location);
}
