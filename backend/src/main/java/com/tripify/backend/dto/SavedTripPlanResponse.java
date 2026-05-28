package com.tripify.backend.dto;

import java.time.Instant;
import java.util.List;

public record SavedTripPlanResponse(
        Long id,
        String city,
        WeatherDto weather,
        List<PlaceDto> places,
        String plan,
        Instant createdAt
) {
}
