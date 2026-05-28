package com.tripify.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaveTripPlanRequest(
        @NotBlank String city,
        @NotNull @Valid WeatherDto weather,
        List<PlaceDto> places,
        @NotBlank String plan
) {
}
