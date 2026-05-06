package com.tripify.backend.dto;

public record WeatherDto(
        double temperature,
        String description
) {
}
