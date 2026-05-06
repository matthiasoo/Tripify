package com.tripify.backend.dto;

import java.util.List;

public record TripPlanResponse(
        String city,
        WeatherDto weather,
        List<PlaceDto> places
) {
}
