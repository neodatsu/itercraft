package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.activity.ActivityService;
import com.itercraft.api.application.activity.ActivitySuggestion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/activities")
public class ActivitiesController {

    private static final Pattern LOCATION_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\s,'-]{1,100}$");

    private final ActivityService activityService;

    public ActivitiesController(ActivityService activityService) {
        this.activityService = activityService;
    }

    private static String validateLocation(String location) {
        if (location == null || !LOCATION_PATTERN.matcher(location).matches()) {
            throw new IllegalArgumentException("Invalid location parameter");
        }
        return location;
    }

    @PostMapping("/suggest")
    public ResponseEntity<ActivitySuggestion> suggestActivities(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam String location) {
        String validatedLocation = validateLocation(location);
        ActivitySuggestion suggestion = activityService.getSuggestions(lat, lon, validatedLocation);
        return ResponseEntity.ok(suggestion);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
