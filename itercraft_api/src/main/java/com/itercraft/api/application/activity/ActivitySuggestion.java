package com.itercraft.api.application.activity;

import java.util.List;
import java.util.Map;

public record ActivitySuggestion(
        String location,
        Map<String, List<Activity>> activities,
        String summary
) {
}
